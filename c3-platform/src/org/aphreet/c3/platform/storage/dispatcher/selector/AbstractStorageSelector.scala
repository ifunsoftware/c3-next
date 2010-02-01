package org.aphreet.c3.platform.storage.dispatcher.selector

import javax.annotation.PostConstruct

abstract class AbstractStorageSelector[T] extends StorageSelector{

  var configAccessor:SelectorConfigAccessor[T] = null
  
  @PostConstruct
  def init = updateConfig(configAccessor.load)
  
  def configEntries:List[(T, String, Boolean)]
  
  def addEntry(entry:(T, String, Boolean)) = {
    configAccessor.update(entries => entries.filter(_._1 != entry._1) + 
    		((entry._1, (entry._2, entry._3)))
    )                       
  }
  
  def removeEntry(key:T) = {
	configAccessor.update(_.filter(_._1 != key))
	updateConfig(configAccessor.load)
  }
  
  protected def updateConfig(config:Map[T, (String, Boolean)])
  
}
