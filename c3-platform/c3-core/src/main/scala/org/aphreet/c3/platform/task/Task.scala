package org.aphreet.c3.platform.task

import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.common.ThreadWatcher

abstract class Task extends Runnable{

  val log = LogFactory getLog getClass

  val id = name + "-" + System.currentTimeMillis

  protected var shouldStopFlag = false

  val SLEEP_ON_PAUSE_INTERVAL = 5000

  private var taskState:TaskState = PENDING

  def state:TaskState = taskState

  override def run() {

    ThreadWatcher + this

    while(!canStart){
      taskState = PENDING
      Thread.sleep(SLEEP_ON_PAUSE_INTERVAL)
    }

    try{
      if(!shouldStop && !Thread.currentThread.isInterrupted){
        taskState = RUNNING
        log.info(id + " started")
        preStart()
        work()
        postComplete()
        taskState = FINISHED
        log.info(id + " stopped")
      }else{
        taskState = INTERRUPTED
        log.info(id + " interrupted") 
      }
    }catch{
      case e: Throwable => {
        taskState = CRASHED
        log error e
        postFailure()
      }
    }finally {
      ThreadWatcher - this
    }
  }

  protected def work(){
    while(!shouldStop && !Thread.currentThread.isInterrupted){
      if(!isPaused){
        step()
      }else Thread.sleep(SLEEP_ON_PAUSE_INTERVAL)
    }
  }

  protected def step() {}

  protected def preStart() {}

  protected def postComplete() {}

  protected def postFailure() {}

  protected def canStart:Boolean = true

  def shouldStop:Boolean = shouldStopFlag

  def name:String = getClass.getSimpleName

  def progress:Int = -1

  def description:TaskDescription = new TaskDescription(id, name, state, progress)

  protected def isPaused:Boolean = {taskState == PAUSED}

  def stop() {
    Thread.currentThread.interrupt()
    shouldStopFlag = true
    taskState = INTERRUPTED
  }

  def pause() {
    if(taskState == RUNNING){
      taskState = PAUSED
    }
  }

  def resume() {
    if(taskState == PAUSED)
      taskState = RUNNING
  }
}

abstract class IterableTask[T] extends Task{

  def createIterator:Iterator[T]

  override def work(){
    val iterator = createIterator

    while(iterator.hasNext && !Thread.currentThread.isInterrupted){
      if(!isPaused){
        processElement(iterator.next())
      }else Thread.sleep(SLEEP_ON_PAUSE_INTERVAL)
    }

    closeIterator()
  }

  def closeIterator(){}

  def processElement(element: T)
}


sealed class TaskState(val name:String, val isFinalState:Boolean);

object RUNNING extends TaskState("Running", false)
object PENDING extends TaskState("Pending", false)
object PAUSED  extends TaskState("Paused", false)
object INTERRUPTED extends TaskState("Interrupted", false)
object FINISHED extends TaskState("Finished", true)
object CRASHED  extends TaskState("Crashed", true)
