package org.aphreet.c3.platform.task.impl

import java.util.concurrent.{ThreadFactory, Executors}
import javax.annotation.{PostConstruct, PreDestroy}
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.task.{TaskManager, Task, TaskDescription}
import org.springframework.stereotype.Component
import scala.collection.mutable


@Component("taskManager")
class TaskManagerImpl extends TaskManager{

  val log = LogFactory getLog getClass
  
  var tasks = new mutable.HashMap[String, Task]

  val threadGroup = new ThreadGroup("C3Tasks")

  val executor = Executors.newCachedThreadPool(new ThreadFactory {
    def newThread(r: Runnable) = {
      val thread = new Thread(threadGroup, r)
      thread.setDaemon(true)
      thread
    }
  })
  
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
  
  @PreDestroy
  def destroy(){

    tasks.foreach(e => e._2.stop())

    executor.shutdown()
    
    log info "Stopping task manager"
  }
}
