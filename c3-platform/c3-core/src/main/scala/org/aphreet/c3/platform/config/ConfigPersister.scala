package org.aphreet.c3.platform.config

/**
 * Author: Mikhail Malygin
 * Date:   12/16/13
 * Time:   12:20 AM
 */
trait ConfigPersister {

  def writeConfig(name: String, config: String)

  def readConfig(name: String): String

  def configExists(name: String): Boolean

}
