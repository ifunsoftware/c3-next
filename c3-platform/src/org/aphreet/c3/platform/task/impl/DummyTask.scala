package org.aphreet.c3.platform.task.impl

import org.apache.commons.logging.LogFactory

class DummyTask extends Task{

  val log = LogFactory getLog getClass
  
  var intProgress:Int = 0;
  
  def runExecution{
    
    while(!(Thread.currentThread.isInterrupted  || progress > 100)){
      if(!isPaused){
    	log info "Dummy task reporting"
    	Thread.sleep(10000)
    	intProgress = intProgress + 1;
      }
    }
    log info "Task Ended"
  }
  
  def name = getClass.getSimpleName
  
  def progress:Int = intProgress;
}
