package org.aphreet.c3.platform.task

import org.aphreet.c3.platform.common.{Logger, CloseableIterable, ThreadWatcher}
import scala.util.control.Exception._

abstract class Task extends Runnable{

  val log = Logger(getClass)

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
        log.error("Task crashed", e)
        e.printStackTrace()
        postFailure()
      }
    }finally {
      ThreadWatcher - this

      handling(classOf[Throwable])
        .by(e => log.warn("Failed to run cleanup handler", e))
        .apply(cleanup())
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

  protected def cleanup() {}

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

abstract class IterableTask[T](val iterable:CloseableIterable[T]) extends Task{

  var processed = 0

  override def work(){
    val iterator = iterable.iterator

    try{
      while(iterator.hasNext && !Thread.currentThread.isInterrupted){
        if(!isPaused){
          processElement(iterator.next())
          processed = processed + 1
        }else {
          Thread.sleep(SLEEP_ON_PAUSE_INTERVAL)
        }
      }
    }finally {
      iterator.close()
    }
  }

  override def progress:Int = {
    ((processed.toFloat / iterable.size) * 100).toInt
  }

  def processElement(element: T)
}


sealed class TaskState(val name:String, val isFinalState:Boolean)

object RUNNING extends TaskState("Running", false)
object PENDING extends TaskState("Pending", false)
object PAUSED  extends TaskState("Paused", false)
object INTERRUPTED extends TaskState("Interrupted", false)
object FINISHED extends TaskState("Finished", true)
object CRASHED  extends TaskState("Crashed", true)
