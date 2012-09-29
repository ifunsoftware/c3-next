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
package org.aphreet.c3.platform.storage.bdb

import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.storage.U
import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.config._
import org.aphreet.c3.platform.storage.common.AbstractStorageFactory

abstract class AbstractBDBStorageFactory extends AbstractStorageFactory with SPlatformPropertyListener{

  val BDB_CONFIG_TX_NO_SYNC = "c3.storage.bdb.txnosync"
  val BDB_CONFIG_CACHE_PERCENT = "c3.storage.bdb.cachepercent"
  val BDB_CONFIG_EMBED_THRESHOLD = "c3.storage.bdb.embedthreshold"
  val BDB_CONFIG_FILE_THRESHOLD = "c3.storage.bdb.filethreshold"

  var currentConfig:BDBConfig = new BDBConfig(false, 20, 10240, 102400)

  var configManager:PlatformConfigManager = _

  @Autowired
  def setPlatformConfigManager(manager:PlatformConfigManager) {configManager = manager}

  def bdbConfig:BDBConfig = currentConfig


  @PostConstruct
  override def init() {
    log info "Post construct callback invoked"
    configManager !? RegisterMsg(this) //sync call. Setting properties before opening storages
    super.init()
  }

  @PreDestroy
  override def destroy() {
    log info "Pre destroy callback invoked"

    letItFall{
      configManager ! UnregisterMsg(this)
    }

    super.destroy()
  }


  def defaultValues:Map[String, String] =
    Map(
      BDB_CONFIG_TX_NO_SYNC -> "false",
      BDB_CONFIG_CACHE_PERCENT -> "20",
      BDB_CONFIG_EMBED_THRESHOLD -> "5120",
      BDB_CONFIG_FILE_THRESHOLD -> "102400"
    )

  def propertyChanged(event: PropertyChangeEvent) {

    def updateStorageParams() {
      log info "Updating storage param " + event.name

      for (storage <- createdStorages) {
        if (storage.isInstanceOf[AbstractSingleInstanceBDBStorage]) {
          val mode = storage.mode
          storage.mode = new U(Constants.STORAGE_MODE_MAINTAIN)
          storage.close()
          storage.asInstanceOf[AbstractSingleInstanceBDBStorage].open(currentConfig)
          storage.mode = mode
        } else if (storage.isInstanceOf[AbstractReplicatedBDBStorage]) {
          val mode = storage.mode
          storage.mode = new U(Constants.STORAGE_MODE_MAINTAIN)
          storage.close()
          storage.asInstanceOf[AbstractReplicatedBDBStorage].open(currentConfig)
          storage.mode = mode
        }
      }

    }

    event.name match {
      case BDB_CONFIG_TX_NO_SYNC => {
        val value = event.newValue == "true"
        if(currentConfig.txNoSync != value){
          currentConfig = new BDBConfig(value, currentConfig.cachePercent, currentConfig.embedThreshold, currentConfig.fileThreshold)
          updateStorageParams()
        }
      }

      case BDB_CONFIG_CACHE_PERCENT => {
        val value = Integer.parseInt(event.newValue)
        if(value != currentConfig.cachePercent){
          currentConfig = new BDBConfig(currentConfig.txNoSync, value, currentConfig.embedThreshold, currentConfig.fileThreshold)
          updateStorageParams()
        }
      }

      case BDB_CONFIG_EMBED_THRESHOLD => {
        val value = Integer.parseInt(event.newValue)
        if(value != currentConfig.embedThreshold){
          currentConfig = new BDBConfig(currentConfig.txNoSync, currentConfig.cachePercent, value, currentConfig.fileThreshold)
          updateStorageParams()
        }
      }

      case BDB_CONFIG_FILE_THRESHOLD => {
        val value = Integer.parseInt(event.newValue)
        if(value != currentConfig.fileThreshold){
          currentConfig = new BDBConfig(currentConfig.txNoSync, currentConfig.cachePercent, currentConfig.embedThreshold, value)
          updateStorageParams()
        }
      }
    }
  }





}