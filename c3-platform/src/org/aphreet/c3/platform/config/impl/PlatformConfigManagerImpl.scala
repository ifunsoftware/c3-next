package org.aphreet.c3.platform.config.impl

/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import org.aphreet.c3.platform.exception.ConfigurationException
import java.io.File
import org.springframework.stereotype.Component
import org.apache.commons.logging.LogFactory
import collection.immutable.Map
import org.aphreet.c3.platform.management.{PropertyChangeEvent, PlatformPropertyListener}
import org.springframework.beans.factory.annotation.Autowired

import java.util.{Set => JSet}
import org.aphreet.c3.platform.config._
import org.aphreet.c3.platform.common.Path

import actors.Actor._
import javax.annotation.{PreDestroy, PostConstruct}
import collection.mutable.{HashMap, HashSet}
import org.aphreet.c3.platform.common.msg.DestroyMsg

@Component("platformConfigManager")
class PlatformConfigManagerImpl extends PlatformConfigManager{
  val log = LogFactory getLog getClass

  var configDir: File = _;

  private var foundListeners = new HashSet[PlatformPropertyListener];

  private val propertyListeners:HashMap[String, Set[PlatformPropertyListener]] = new HashMap;

  private var currentConfig: Map[String, String] = null;

  var configAccessor: PlatformConfigAccessor = _


  @Autowired
  def setConfigAccessor(accessor: PlatformConfigAccessor) = {
    configAccessor = accessor
  }

  @Autowired {val required = false}
  def setPlatformPropertyListeners(listeners: JSet[PlatformPropertyListener]) = {
    foundListeners ++ Set.apply(listeners)
  }


  @PostConstruct
  def init = {
    var configPath = System.getProperty("c3.home")

    if (configPath == null) {
      log info "Config path is not set. Using user home path"
      configPath = System.getProperty("user.home") + File.separator + ".c3"
    }

    if (configPath == null) {
      throw new ConfigurationException("Can't find path to store config")
    } else {
      log info "Using " + configPath + " to store C3 configuration"
    }

    val path = new Path(configPath)

    configPath = path.toString
    configDir = path.file
    if (!configDir.exists) configDir.mkdirs

    configAccessor.configDirectory = configDir

    //Starting listening for events
    log info "Starting config manager actor"
    this.start


    if (foundListeners != null)
      foundListeners.foreach { 
        this ! new RegisterMsg(_)
      }

  }

  @PreDestroy
  def destroy = {
    this ! DestroyMsg
  }

  def act {
    loop {
      react {
        case RegisterMsg(listener) => {
          log info "Registering property listener: " + listener.getClass.getSimpleName

          for (paramName <- listener.listeningForProperties) {
            propertyListeners.get(paramName) match {
              case Some(regListeners) => propertyListeners.put(paramName, regListeners + listener)
              case None => propertyListeners.put(paramName, Set(listener))
            }

            getPlatformProperties.get(paramName) match {
              case Some(currentValue) =>
                listener.propertyChanged(new PropertyChangeEvent(paramName, null, currentValue, this))
              case None =>
                val defaultParamValue = listener.defaultPropertyValues.get(paramName)
                if (defaultParamValue != null)
                  setPlatformProperty(paramName, defaultParamValue)
            }
          }

          log debug propertyListeners.toString
        }

        case UnregisterMsg(listener) => {

          log info "Registering property listener: " + listener.getClass.getSimpleName

          for ((prop, listeners) <- propertyListeners if listeners.contains(listener)) {

            propertyListeners.put(prop, listeners - listener)

          }

          log debug propertyListeners.toString
        }

        case SetPropertyMsg(key, value) => {
          log info "Setting platform property: " + key

          var config = configAccessor.load

          val oldValue: String = config.get(key) match {
            case Some(v) => v
            case None => null
          }

          if(oldValue == null || oldValue != value){

            try {
              propertyListeners.get(key) match {
                case Some(lx) => for (l <- lx)
                  l.propertyChanged(new PropertyChangeEvent(key, oldValue, value, this))
                case None =>
              }

              currentConfig = config + ((key, value))

              configAccessor store currentConfig
            } catch {
              case e =>
                log.warn("Failed to set property " + key, e)
            }
          }else log info "The value of the property " + key + " did not change"
        }

        case DestroyMsg => {
          log info "Stopping config manager actor..."
          exit
        }
      }
    }
  }

  override
  def registerPropertyListener(listener: PlatformPropertyListener) = {

    this ! RegisterMsg(listener)

  }

  override
  def unregisterPropertyListener(listener: PlatformPropertyListener) = {

    this ! UnregisterMsg(listener)

  }


  def getPlatformProperties: Map[String, String] = {

    if (currentConfig == null) {
      currentConfig = configAccessor.load
    }
    //Collections.unmodifiableMap[String, String](currentConfig)
    currentConfig
  }


  override
  def setPlatformProperty(key: String, value: String) = {

    this ! SetPropertyMsg(key, value)

  }
}

