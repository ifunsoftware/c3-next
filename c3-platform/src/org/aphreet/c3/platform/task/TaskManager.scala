package org.aphreet.c3.platform.task

trait TaskManager {

  def taskList:List[TaskDescription]
  
  def stopTask(id:String)
  
  def pauseTask(id:String)
  
  def resumeTask(id:String)
  
  def submitTask(task:Task)
  
}
