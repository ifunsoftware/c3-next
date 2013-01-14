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
import org.springframework.beans.factory.annotation.Autowired

import java.util.{Set => JSet}
import org.aphreet.c3.platform.config._
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg.{DoneMsg, DestroyMsg}
import org.aphreet.c3.platform.common.{Constants, Path}
import org.aphreet.c3.platform.resource.IdGenerator
import collection.mutable

@Component("platformConfigManager")
class PlatformConfigManagerImpl extends PlatformConfigManager{
  val log = LogFactory getLog getClass

  var configDir: File = _

  private val foundListeners = new mutable.HashSet[PlatformPropertyListener]

  private val propertyListeners = new mutable.HashMap[String, Set[PlatformPropertyListener]]

  private var currentConfig: Map[String, String] = null

  var configAccessor: PlatformConfigAccessor = _


  @Autowired
  def setConfigAccessor(accessor: PlatformConfigAccessor) {
    configAccessor = accessor
  }

  @Autowired (required = false)
  def setPlatformPropertyListeners(listeners: JSet[PlatformPropertyListener]) = {
    foundListeners ++ Set.apply(listeners)
  }

  {
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
  }

  @PostConstruct
  def init() {
   
    configAccessor.configDir = configDir

    //before actor start verify that systemId property exists in config
    //if no, create new one

    val config = configAccessor.load

    config.get(Constants.C3_SYSTEM_ID) match{
      case None => {
        log info "Generating new system id"
        configAccessor.store(
          config + ((Constants.C3_SYSTEM_ID, IdGenerator.generateSystemId))
        )
      }
      case Some(x) => log info "Found systemId " + x
    }

    
    //Starting listening for events
    log info "Starting config manager actor"
    this.start()


    if (foundListeners != null)
      foundListeners.foreach { 
        this ! new RegisterMsg(_)
      }

  }

  @PreDestroy
  def destroy() {
    log info "Stopping PlatformConfigManager"
    this ! DestroyMsg
  }

  def act() {
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
          reply{
            DoneMsg
          }
          log debug propertyListeners.toString
        }

        case UnregisterMsg(listener) => {

          log info "Unregistering property listener: " + listener.getClass.getSimpleName

          for ((prop, listeners) <- propertyListeners if listeners.contains(listener)) {

            propertyListeners.put(prop, listeners - listener)

          }

          log debug propertyListeners.toString
        }

        case SetPropertyMsg(key, value) => {
          log info "Setting platform property: " + key

          val config = configAccessor.load

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
              case e: Throwable =>
                log.warn("Failed to set property " + key, e)
            }
          }else log info "The value of the property " + key + " did not change"
        }

        case DestroyMsg => {
          log info "Stopped config manager actor"
          this.exit()
        }
      }
    }
  }

  def getSystemId:String = {
    getPlatformProperties.get(Constants.C3_SYSTEM_ID) match{
      case Some(id) => id
      case None => null
    }
  }


  def getPlatformProperties: Map[String, String] = {

    if (currentConfig == null) {
      currentConfig = configAccessor.load
    }
    currentConfig
  }


  override
  def setPlatformProperty(key: String, value: String) {

    this ! SetPropertyMsg(key, value)

  }
}

