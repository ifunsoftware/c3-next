/*
 * Copyright (c) 2012, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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