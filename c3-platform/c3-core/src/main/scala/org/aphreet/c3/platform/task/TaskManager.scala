package org.aphreet.c3.platform.task

trait TaskManager {

  def taskList:List[TaskDescription]

  def finishedTaskList:List[TaskDescription]
  
  def stopTask(id:String)
  
  def pauseTask(id:String)
  
  def resumeTask(id:String)
  
  def submitTask(task:Task):String

  def getTaskById(id: String) : Task


  def scheduleTask(task: Task, crontabSchedule: String)

  def rescheduleTask(id: String, crontabSchedule: String)

  def removeScheduledTask(id: String)

  def scheduledTaskList: List[TaskDescription]
  
}
