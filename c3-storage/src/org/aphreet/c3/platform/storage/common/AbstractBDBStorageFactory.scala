package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.management.{PlatformManagementEndpoint, PropertyChangeEvent, SPlatformPropertyListener}
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.storage.U

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

  var management:PlatformManagementEndpoint = _

  @Autowired
  def setPlatformManagementEndpoint(endpoint:PlatformManagementEndpoint) = {management = endpoint}


  def bdbConfig:BDBConfig = currentConfig


  @PostConstruct
  override def init = {
    log info "Post construct callback invoked"
    management.registerPropertyListener(this)
    super.init
  }

  @PreDestroy
  override def destroy = {
    log info "Pre destroy callback invoked"
    management.unregisterPropertyListener(this)
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



  def propertyChanged(event:PropertyChangeEvent) = {
    event.name match {
      case BDB_CONFIG_TX_NO_SYNC => {
          val value = event.newValue == "true"
          currentConfig = new BDBConfig(value, currentConfig.txWriteNoSync, currentConfig.cachePercent)
        }

      case BDB_CONFIG_TX_WRITE_NO_SYNC => {
          val value = event.newValue == "true"
          currentConfig = new BDBConfig(currentConfig.txNoSync, value, currentConfig.cachePercent)
        }

      case BDB_CONFIG_CACHE_PERCENT => {
          val value = Integer.parseInt(event.newValue)
          currentConfig = new BDBConfig(currentConfig.txNoSync, currentConfig.txWriteNoSync, value)
        }
    }
    log info "Updating storage param " + event.name

    for(storage <- createdStorages if (storage.isInstanceOf[AbstractBDBStorage])){
      val mode = storage.mode
      storage.mode = new U(Constants.STORAGE_MODE_MAINTAIN)
      storage.close
      storage.asInstanceOf[AbstractBDBStorage].open(bdbConfig)
      storage.mode = mode
    }

  }





}