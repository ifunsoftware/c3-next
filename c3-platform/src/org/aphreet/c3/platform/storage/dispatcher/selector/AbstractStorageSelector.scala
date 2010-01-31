package org.aphreet.c3.platform.storage.dispatcher.selector

abstract class AbstractStorageSelector[T] extends StorageSelector{

  var configAccessor:SelectorConfigAccessor[T] = null
  
  
  
  def configEntries:List[(String, String, Boolean)];
  
  def addEn
}
