package org.aphreet.c3.platform.storage.dispatcher.selector

/**
 * Author: Mikhail Malygin
 * Date:   12/9/13
 * Time:   9:19 PM
 */
trait ModifiableStorageSelector[T] extends StorageSelector {

  def configEntries:List[(T, Boolean)]

  def addEntry(entry:(T, Boolean))

  def removeEntry(key:T)

}
