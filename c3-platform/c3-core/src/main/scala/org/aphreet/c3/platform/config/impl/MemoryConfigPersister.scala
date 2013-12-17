package org.aphreet.c3.platform.config.impl

import org.aphreet.c3.platform.config.ConfigPersister
import scala.collection.mutable

/**
 * Author: Mikhail Malygin
 * Date:   12/16/13
 * Time:   4:44 PM
 */
class MemoryConfigPersister extends ConfigPersister{

  private val configs = new mutable.HashMap[String, String]()

  def writeConfig(name: String, config: String): Unit = configs.synchronized(configs.put(name, config))

  def readConfig(name: String): String = configs.synchronized(configs.get(name).get)

  def configExists(name: String) = configs.synchronized(configs.contains(name))

}
