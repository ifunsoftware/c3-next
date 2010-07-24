package org.aphreet.c3.platform.task.impl

import javax.annotation.{PostConstruct, PreDestroy}

import scala.collection.mutable.HashMap

import java.util.concurrent.Executors

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.task.{TaskManager, Task, TaskDescription}

@Component("taskManager")
class TaskManagerImpl extends TaskManager{

  val log = LogFactory getLog getClass
  
  var tasks = new HashMap[String, Task]
  
  val executor = Executors.newCachedThreadPool
  
  @PostConstruct
  def init{
    log info "Starting task manager"
  }
  
  def taskList:List[TaskDescription] = {
    (for((taskId, task) <- tasks if !task.state.isFinalState)
      yield task.description).elements.toList
  }

  def finishedTaskList:List[TaskDescription] = 
    (for((taskId, task) <- tasks if task.state.isFinalState)
      yield task.description).elements.toList
  
  def stopTask(id:String)  = 
    tasks.get(id) match {
      case Some(task) => task.stop
      case None => log warn "Can't stop task with id " + id + ": task does not exist"
    }
    
  
  def pauseTask(id:String) = 
    tasks.get(id) match {
      case Some(task) => task.pause
      case None => log warn "Can't pause task with id " + id + ": task does not exist"
    }
  
  def resumeTask(id:String) = 
    tasks.get(id) match {
      case Some(task) => task.resume
      case None => log warn "Can't resume task with id " + id + ": task does not exist"
    }
  
  def submitTask(task:Task):String =
    this.synchronized{
      executor submit task
      tasks.put(task.id, task)

      log debug "Submitted task " + task.id
      task.id
    }
  
  @PreDestroy
  def destroy{

    tasks.foreach(e => e._2.stop)

    executor.shutdown
    
    log info "Stopping task manager"
  }
}
