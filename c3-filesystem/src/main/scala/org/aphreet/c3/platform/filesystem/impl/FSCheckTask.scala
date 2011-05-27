/**
 * Copyright (c) 2011, Mikhail Malygin
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

import org.aphreet.c3.platform.task.Task
import org.aphreet.c3.platform.access.AccessManager
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import org.aphreet.c3.platform.filesystem.{Node, Directory}
import org.aphreet.c3.platform.statistics.{ResetStatisticsMsg, IncreaseStatisticsMsg, StatisticsManager}

class FSCheckTask(val accessManager:AccessManager,
                  val statisticsManager:StatisticsManager,
                  val fsRoots:Map[String, String]) extends Task{

  var rootListToCheck = List[String]()

  override def preStart {
    statisticsManager ! ResetStatisticsMsg("c3.filesystem.check.found")
    statisticsManager ! ResetStatisticsMsg("c3.filesystem.check.failure")
    statisticsManager ! ResetStatisticsMsg("c3.filesystem.check.total")

    rootListToCheck = fsRoots.map(e => e._2).toList
  }

  override def step {
    
    rootListToCheck.headOption match{
      case Some(currentRoot) => {
        rootListToCheck = rootListToCheck.tail
        val resource = accessManager.get(currentRoot)
        val node = Node.fromResource(resource)

        if(node.isDirectory)
          checkDirectoryContents(node.asInstanceOf[Directory])
      }
      case None => {
        shouldStopFlag = true
      }
    }
  }

  override def progress:Int = {
    val totalRootsToCheck = fsRoots.size

    ((totalRootsToCheck - rootListToCheck.size).toFloat / totalRootsToCheck).toInt * 100
  }

  def checkDirectoryContents(directory:Directory):Unit = {

    log debug "Checking directory " + directory.resource.address

    for(child <- directory.getChildren){

      val address = child.address

      try{
        statisticsManager ! IncreaseStatisticsMsg("c3.filesystem.check.total", 1)
        if(log.isTraceEnabled){
          log trace "Checking child " + address
        }
        accessManager.get(address)
      }catch{
        case e:ResourceNotFoundException => {
          directory.removeChild(child.name)
          log debug "Removed missing resource " + child.name + "(" + address + ") from directory " + directory.resource.address
          statisticsManager ! IncreaseStatisticsMsg("c3.filesystem.check.found", 1)
        }

        case e => {
          log warn "Failed to get resource " + address
          statisticsManager ! IncreaseStatisticsMsg("c3.filesystem.check.failure", 1)
        }
      }
    }

    accessManager.update(directory.resource)

    log debug "Directory check complete " + directory.resource.address

    for(child <- directory.getChildren if !child.leaf){
      val resource = accessManager.get(child.address)
      val node = Node.fromResource(resource)

      if(node.isDirectory)
        checkDirectoryContents(node.asInstanceOf[Directory])
    }

  }

}