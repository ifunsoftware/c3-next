package org.aphreet.c3.platform.filesystem.impl

import org.springframework.stereotype.Component
import javax.annotation.{PreDestroy, PostConstruct}
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.common.msg.DestroyMsg
import collection.mutable
import org.aphreet.c3.platform.filesystem._
import actors.Actor
import org.aphreet.c3.platform.common.WatchedActor
import org.aphreet.c3.platform.filesystem.ScheduleMsg
import org.aphreet.c3.platform.filesystem.RunTasks
import org.aphreet.c3.platform.filesystem.FSDirectoryTask
import org.aphreet.c3.platform.filesystem.TaskDoneMsg
import scala.Some
import org.springframework.beans.factory.annotation.Autowired

@Component
class FSDirectoryUpdaterImpl extends FSDirectoryUpdater{

  val log = LogFactory.getLog(getClass)

  @Autowired
  var fsManager:FSManagerInternal = null

  private val scheduledTasksMap = new mutable.HashMap[String, List[FSDirectoryTask]]
  private val runningTasks = new mutable.HashMap[String, Actor]
  private var freeActors = List[Actor]()

  @PostConstruct
  def init(){

    log.info("Starting DirectoryUpdater")

    for(i <- 1 to 5){
      freeActors = new DirectoryTaskActor(this) :: freeActors
    }

    freeActors.foreach(_.start())

    this.start()
  }

  @PreDestroy
  def destroy(){
    this ! DestroyMsg
  }

  override def act(){
    loop{
      react{
        case ScheduleMsg(directoryAddress, task) => {

          scheduledTasksMap.get(directoryAddress) match {
            case Some(taskList) => scheduledTasksMap.put(directoryAddress, task :: taskList)
            case None => scheduledTasksMap.put(directoryAddress, List(task))
          }

          scheduleTaskIfPossible()
        }

        case TaskDoneMsg(address, actor) => {

          runningTasks.remove(address)
          freeActors = actor :: freeActors

          scheduleTaskIfPossible()
        }

        case DestroyMsg => {
          freeActors.foreach(_ ! DestroyMsg)
          freeActors = List()

          runningTasks.values.foreach(_ ! DestroyMsg)

          exit()
        }
      }
    }
  }

  protected def scheduleTaskIfPossible(){

    freeActors.headOption match {
      case Some(actor) => {

        scheduledTasksMap.filterKeys(!runningTasks.contains(_)).headOption match {
          case Some(addressTask) => {
            scheduledTasksMap.remove(addressTask._1)
            freeActors = freeActors.tail
            actor ! RunTasks(addressTask._1, addressTask._2)
            runningTasks.put(addressTask._1, actor)
          }
          case None =>
        }
      }
      case None =>
    }
  }
}

class DirectoryTaskActor(val directoryUpdater:FSDirectoryUpdaterImpl) extends WatchedActor{

  override def act(){
    loop {
      react{
        case RunTasks(address, taskList) => {
          directoryUpdater.fsManager.executeDirectoryTasks(address, taskList)

          directoryUpdater ! TaskDoneMsg(address, this)
        }

        case DestroyMsg => exit()
      }
    }
  }
}