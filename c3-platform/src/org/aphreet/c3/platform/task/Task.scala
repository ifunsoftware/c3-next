package org.aphreet.c3.platform.task

import org.apache.commons.logging.LogFactory

abstract class Task extends Runnable{

  val log = LogFactory getLog getClass
  
  val id = name + "-" + System.currentTimeMillis
  
  
  private val SLEEP_ON_PAUSE_INTERVAL = 5000
  
  private var taskState:TaskState = PENDING

  def state:TaskState = taskState

  override def run = {
    taskState = RUNNING
    
    try{
      preStart
      while(!shouldStop && !Thread.currentThread.isInterrupted){
        if(!isPaused){
          step
        }else Thread.sleep(SLEEP_ON_PAUSE_INTERVAL)
      }
      postComplete
      taskState = FINISHED
    }catch{
      case e => {
        taskState = CRASHED
        log error e
        postFailure
      }
    }
  }
  
  protected def step;
  
  protected def preStart = {};
  
  protected def postComplete = {};
  
  protected def postFailure = {}
  
  def shouldStop:Boolean = false
  
  def name:String = getClass.getSimpleName
  
  def progress:Int = -1

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
