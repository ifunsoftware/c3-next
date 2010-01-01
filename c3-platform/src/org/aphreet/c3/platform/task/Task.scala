package org.aphreet.c3.platform.task

abstract class Task extends Runnable{

  val id = generateId
  
  private def generateId:String = name + "-" + System.currentTimeMillis
  
  
  private var taskState:TaskState = PENDING

  def state:TaskState = taskState

  
  
  override def run = {
    taskState = RUNNING
    
    try{
      runExecution
      taskState = FINISHED
    }catch{
      case e => taskState = CRASHED
    }
  }
  
  protected def runExecution;
  
  def name:String
  
  def progress:Int

  def description:TaskDescription = new TaskDescription(id, name, state, progress)
  
  protected def isPaused:Boolean = {taskState == PAUSED}
  
  def stop = {
    Thread.currentThread.interrupt
    taskState = INTERRUPTED
  }
  
  def pause = {
    if(taskState == RUNNING){
      taskState = PAUSED
    }
  }
  
  def resume = {
    if(taskState == PAUSED)
      taskState = RUNNING
  }
}


sealed class TaskState(val name:String);

object RUNNING extends TaskState("Running")
object PENDING extends TaskState("Pending")
object PAUSED  extends TaskState("Paused")
object INTERRUPTED extends TaskState("Interrupted")
object FINISHED extends TaskState("Finished")
object CRASHED  extends TaskState("Crashed")