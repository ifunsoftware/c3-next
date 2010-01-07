package org.aphreet.c3.platform.task.impl

class DummyTask extends Task{
  
  var intProgress:Int = 0;
  
  override def step = {
    log info "Dummy task reporting"
    Thread.sleep(10000)
    intProgress = intProgress + 1;
  }
  
  override def postComplete = {
    log info "Task Ended"
  }
  
  override def shouldStop:Boolean = progress >= 100
  
  override def progress = intProgress
  
}
