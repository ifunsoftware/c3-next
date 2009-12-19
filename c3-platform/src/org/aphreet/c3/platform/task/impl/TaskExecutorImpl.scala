package org.aphreet.c3.platform.task.impl

class TaskExecutorImpl extends TaskExecutor{

  
  
  def taskList:List[TaskDescription] = {
   List() 
  }
  
  def stopTask(id:String)  = {}
  
  def pauseTask(id:String) = {}
  
  def resumeTask(id:String) = {}
  
  def submitTask(task:Task) = {}
}
