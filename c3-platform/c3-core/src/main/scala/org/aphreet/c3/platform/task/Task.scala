package org.aphreet.c3.platform.task

import org.aphreet.c3.platform.common._
import scala.util.control.Exception._
import java.util.concurrent.Future
import org.aphreet.c3.platform.exception.PlatformException

abstract class Task extends Runnable {

  val log = Logger(getClass)

  val id = name + "-" + System.currentTimeMillis

  private var schedule = ""

  protected var shouldStopFlag = false

  protected var restartableFlag = false

  val SLEEP_ON_PAUSE_INTERVAL = 5000

  private var taskState: TaskState = PENDING

  private var observers = List.empty[TaskScheduleObserver]

  def state: TaskState = taskState

  var taskFuture: Future[_] = null

  override def run() {

    ThreadWatcher + this

    while (!canStart) {
      taskState = PENDING
      Thread.sleep(SLEEP_ON_PAUSE_INTERVAL)
    }

    try {
      if (!shouldStop && !Thread.currentThread.isInterrupted) {
        taskState = RUNNING
        log.info(id + " started")
        preStart()

        try {
          work()
        } catch {
          case e: InterruptedException => {
            if (!shouldStop) {
              throw new PlatformException("Unexpected interrupted event for task " + id, e)
            }
          }
        }
        postComplete()
        taskState = FINISHED
        log.info(id + " stopped")
      } else {
        taskState = INTERRUPTED
        log.info(id + " interrupted")
      }
    } catch {
      case e: Throwable => {
        taskState = CRASHED
        log.error("Task crashed", e)
        e.printStackTrace()
        postFailure()
      }
    } finally {
      if (isRestartable) {
        shouldStopFlag = false
      }
      ThreadWatcher - this

      handling(classOf[Throwable])
        .by(e => log.warn("Failed to run cleanup handler", e))
        .apply(cleanup())
    }
  }

  protected def work() {
    while (!shouldStop && !Thread.currentThread.isInterrupted) {
      if (!isPaused) {
        step()
      } else Thread.sleep(SLEEP_ON_PAUSE_INTERVAL)
    }
  }

  protected def step() {}

  protected def preStart() {}

  protected def postComplete() {}

  protected def postFailure() {}

  protected def cleanup() {}

  protected def canStart: Boolean = true

  def shouldStop: Boolean = shouldStopFlag

  def name: String = getClass.getSimpleName

  def progress: Int = -1

  def description: TaskDescription = new TaskDescription(id, name, state, progress, schedule)

  protected def isPaused: Boolean = {
    taskState == PAUSED
  }

  def isRestartable: Boolean = restartableFlag

  def setRestartable(restartable: Boolean) {
    restartableFlag = restartable
  }

  def getSchedule: String = schedule

  def setSchedule(newSchedule: String) {
    observers.foreach(observer => observer.updateSchedule(this, newSchedule))

    schedule = newSchedule
  }

  def addObserver(observer: TaskScheduleObserver) {
    observers ::= observer
  }

  def removeObserver(observer: TaskScheduleObserver) {
    observers = observers diff List(observer)
  }

  def stop() {

    if (taskFuture != null) {
      taskFuture.cancel(true)
    }

    shouldStopFlag = true
    taskState = INTERRUPTED
  }

  def pause() {
    if (taskState == RUNNING) {
      taskState = PAUSED
    }
  }

  def resume() {
    if (taskState == PAUSED)
      taskState = RUNNING
  }
}

abstract class IteratorTask[T](val iteratorFunction: () => CountingIterator[T]) extends Task {

  private var progressFetcher: Option[() => Int] = None

  override def work() {

    val iterator = iteratorFunction()

    progressFetcher = Some(() => iterator.progress)

    try {
      while (iterator.hasNext && !Thread.currentThread.isInterrupted) {
        if (!isPaused) {
          processElement(iterator.next())
          throttle()
        } else {
          Thread.sleep(SLEEP_ON_PAUSE_INTERVAL)
        }
      }
    } finally {
      iterator.close()
    }
  }

  override def progress: Int = {
    progressFetcher.map(_()).getOrElse(-1)
  }

  def processElement(element: T)

  def throttle() {}

}

abstract class IterableTask[T](val iterable: CloseableIterable[T])
  extends IteratorTask(() => new SimpleCountingIterator(iterable.iterator, iterable.size)) {
}


sealed class TaskState(val name: String, val isFinalState: Boolean)

object RUNNING extends TaskState("Running", false)

object PENDING extends TaskState("Pending", false)

object PAUSED extends TaskState("Paused", false)

object INTERRUPTED extends TaskState("Interrupted", false)

object FINISHED extends TaskState("Finished", true)

object CRASHED extends TaskState("Crashed", true)
