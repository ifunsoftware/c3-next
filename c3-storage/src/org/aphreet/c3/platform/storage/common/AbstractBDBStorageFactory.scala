package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.management.{PlatformManagementEndpoint, PropertyChangeEvent, SPlatformPropertyListener}
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.storage.U
import org.aphreet.c3.platform.config.{UnregisterEvent, RegisterEvent, PlatformConfigManager}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 11, 2010
 * Time: 1:12:41 AM
 * To change this template use File | Settings | File Templates.
 */

abstract class AbstractBDBStorageFactory extends AbstractStorageFactory with SPlatformPropertyListener{

  val BDB_CONFIG_TX_NO_SYNC = "c3.storage.bdb.txnosync"
  val BDB_CONFIG_TX_WRITE_NO_SYNC = "c3.storage.bdb.txwritenosync"
  val BDB_CONFIG_CACHE_PERCENT = "c3.storage.bdb.cachepercent"

  var currentConfig:BDBConfig = new BDBConfig(false, false, 20)

  var configManager:PlatformConfigManager = _

  @Autowired
  def setPlatformConfigManager(manager:PlatformConfigManager) = {configManager = manager}

  def bdbConfig:BDBConfig = currentConfig


  @PostConstruct
  override def init = {
    super.init

    log info "Post construct callback invoked"

    configManager ! RegisterEvent(this)
  }

  @PreDestroy
  override def destroy = {
    log info "Pre destroy callback invoked"

    configManager ! UnregisterEvent(this)

    super.destroy
  }


  def defaultValues:Map[String, String] =
    Map(
      BDB_CONFIG_TX_NO_SYNC -> "false",
      BDB_CONFIG_TX_WRITE_NO_SYNC -> "false",
      BDB_CONFIG_CACHE_PERCENT -> "20"
      )

  def listeningForProperties:Array[String] =
    Array(BDB_CONFIG_CACHE_PERCENT, BDB_CONFIG_TX_NO_SYNC, BDB_CONFIG_TX_WRITE_NO_SYNC)


  def propertyChanged(event: PropertyChangeEvent) = {

    def updateStorageParams {
      log info "Updating storage param " + event.name

      for (storage <- createdStorages if (storage.isInstanceOf[AbstractBDBStorage])) {
        val mode = storage.mode
        storage.mode = new U(Constants.STORAGE_MODE_MAINTAIN)
        storage.close
        storage.asInstanceOf[AbstractBDBStorage].open(currentConfig)
        storage.mode = mode
      }
    }


    event.name match {
      case BDB_CONFIG_TX_NO_SYNC => {
        val value = event.newValue == "true"
        if(currentConfig.txNoSync != value){
          currentConfig = new BDBConfig(value, currentConfig.txWriteNoSync, currentConfig.cachePercent)
          updateStorageParams
        }
      }

      case BDB_CONFIG_TX_WRITE_NO_SYNC => {
        val value = event.newValue == "true"

        if(currentConfig.txWriteNoSync != value){
          currentConfig = new BDBConfig(currentConfig.txNoSync, value, currentConfig.cachePercent)
          updateStorageParams
        }


      }

      case BDB_CONFIG_CACHE_PERCENT => {
        val value = Integer.parseInt(event.newValue)
        if(value != currentConfig.cachePercent){
          currentConfig = new BDBConfig(currentConfig.txNoSync, currentConfig.txWriteNoSync, value)
          updateStorageParams
        }
      }
    }


  }





}