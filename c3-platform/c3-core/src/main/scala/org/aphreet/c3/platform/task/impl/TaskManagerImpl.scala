package org.aphreet.c3.platform.task.impl

import java.util.concurrent.{ScheduledFuture, ThreadFactory, Executors}
import javax.annotation.{PostConstruct, PreDestroy}
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.task.{TaskManager, Task, TaskDescription}
import org.springframework.stereotype.Component
import scala.collection.mutable
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import org.springframework.scheduling.{Trigger, TaskScheduler}
import java.util.Date
import org.springframework.scheduling.support.{CronTrigger, PeriodicTrigger}


@Component("taskManager")
class TaskManagerImpl extends TaskManager{

  val log = LogFactory getLog getClass
  
  var tasks = new mutable.HashMap[String, Task]

  var tasksFutures = new mutable.HashMap[String, ScheduledFuture[_]]

  val threadGroup = new ThreadGroup("C3Tasks")

  val executor = Executors.newScheduledThreadPool(10, new ThreadFactory {
    def newThread(r: Runnable) = {
      val thread = new Thread(threadGroup, r)
      thread.setDaemon(true)
      thread
    }
  })

  val scheduler : TaskScheduler = new ConcurrentTaskScheduler(executor)

  
  @PostConstruct
  def init(){
    log info "Starting task manager"
  }
  
  def taskList:List[TaskDescription] = {
    (for((taskId, task) <- tasks if !task.state.isFinalState)
      yield task.description).toList
  }

  def finishedTaskList:List[TaskDescription] = 
    (for((taskId, task) <- tasks if task.state.isFinalState)
      yield task.description).toList
  
  def stopTask(id:String) {
    tasks.get(id) match {
      case Some(task) => task.stop()
      case None => log warn "Can't stop task with id " + id + ": task does not exist"
    }
  }
    
  
  def pauseTask(id:String) {
    tasks.get(id) match {
      case Some(task) => task.pause()
      case None => log warn "Can't pause task with id " + id + ": task does not exist"
    }
  }
  
  def resumeTask(id:String) {
    tasks.get(id) match {
      case Some(task) => task.resume()
      case None => log warn "Can't resume task with id " + id + ": task does not exist"
    }
  }
  
  def submitTask(task:Task):String =
    this.synchronized{
      executor submit task
      tasks.put(task.id, task)

      log debug "Submitted task " + task.id
      task.id
    }

  def getTaskById(id: String) : Task = {
    tasks.get(id) match {
      case Some(task) => task
      case None => throw new IllegalStateException("Task " + id + " doesn't exist")
    }
  }

  def scheduleTask(task: Task, crontabSchedule: String) {
    task.setRestartable(true)

    scheduleTask(task, new CronTrigger(crontabSchedule))

    task.schedule = crontabSchedule

    log.debug("Task " + task.id + " was scheduled: " + crontabSchedule)
  }

  def scheduleTask(task: Task, period: Long) {
    scheduleTask(task, period, 0)
  }

  def scheduleTask(task: Task, period: Long, startDelay: Long) {
    scheduleTask(task, period, startDelay, true)
  }

  def scheduleTask(task: Task, period: Long, startDelay: Long, fixedPeriod: Boolean) {
    val trigger = new PeriodicTrigger(period)
    trigger.setInitialDelay(startDelay)
    trigger.setFixedRate(fixedPeriod)

    scheduleTask(task, trigger)

    log.debug("Task " + task.id + " was scheduled for execution every " + period
      + " ms with initial delay " + startDelay + " ms")
  }

  private def scheduleTask(task: Task, trigger: Trigger) {
    val scheduledFuture = scheduler.schedule(task, trigger)

    tasks.put(task.id, task)
    tasksFutures.put(task.id, scheduledFuture)
  }

  def rescheduleTask(id: String, crontabSchedule: String) {
    tasksFutures.get(id) match {

      case Some(taskFuture) => {
        taskFuture.cancel(false)
        tasksFutures.remove(id)

        val task = tasks.get(id).get
        val scheduledFuture = scheduler.schedule(task, new CronTrigger(crontabSchedule))
        task.schedule = crontabSchedule
        tasksFutures.put(task.id, scheduledFuture)

        log.debug("Task " + id + " was rescheduled: " + crontabSchedule)
      }

      case None => log warn "Can't reschedule task with id " + id + ": task was not scheduled before"
    }
  }

  def removeScheduledTask(id: String) {
    tasksFutures.get(id) match {

      case Some(taskFuture) => {
        taskFuture.cancel(false)  //remove from schedule, if task's runnning, dont't interrupt it
        tasksFutures.remove(id)

        if (tasks.contains(id)) {
          tasks.remove(id)
        }

        log.debug("Task " + id + " was removed from the schedule")
      }

      case None => log warn "Can't remove task with id " + id + " from the schedule: task was not scheduled before"
    }
  }

  def scheduledTaskList = {
    (for((taskId, task) <- tasks if tasksFutures.contains(taskId))
        yield task.description).toList
  }

  @PreDestroy
  def destroy(){

    tasks.foreach(e => e._2.stop())

    executor.shutdown()
    
    log info "Stopping task manager"
  }
}
