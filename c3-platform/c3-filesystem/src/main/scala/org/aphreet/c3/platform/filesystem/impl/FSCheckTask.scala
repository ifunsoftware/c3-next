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

import org.aphreet.c3.platform.access.AccessManager
import org.aphreet.c3.platform.exception.ResourceNotFoundException
import org.aphreet.c3.platform.filesystem.{Node, Directory}
import org.aphreet.c3.platform.query.{GenericQueryConsumer, QueryManager}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.statistics.{ResetStatisticsMsg, IncreaseStatisticsMsg, StatisticsManager}
import org.aphreet.c3.platform.task.Task

class FSCheckTask(val accessManager: AccessManager,
                  val statisticsManager: StatisticsManager,
                  val queryManager: QueryManager,
                  val fsManager: FSManagerImpl,
                  val fsRoots: Map[String, String]) extends Task {

  var rootsToCheck = Map[String, String]()

  override def preStart {
    statisticsManager ! ResetStatisticsMsg("c3.filesystem.check.found")
    statisticsManager ! ResetStatisticsMsg("c3.filesystem.check.failure")
    statisticsManager ! ResetStatisticsMsg("c3.filesystem.check.total")
    statisticsManager ! ResetStatisticsMsg("c3.filesystem.check.root.found")
    statisticsManager ! ResetStatisticsMsg("c3.filesystem.check.root.fixed")

    rootsToCheck = fsRoots
  }

  override def step {

    rootsToCheck.headOption match {
      case Some(currentRoot) => {
        rootsToCheck = rootsToCheck.tail
        val resource = accessManager.get(currentRoot._2)

        val domain = currentRoot._1

        val correctRoot = if (resource.systemMetadata("c3.domain.id").get != domain) {
          log.warn("Found incorrect root " + currentRoot._2 + " for domain " + domain)

          statisticsManager ! IncreaseStatisticsMsg("c3.filesystem.check.root.found", 1)

          log.info("Looking for correct root for domain " + domain)

          findCorrectRoot(domain) match {
            case Some(address) => {
              log.info("Found new root for domain: " + domain + " with address " + address)
              fsManager.overrideFileSystemRoot(domain, address)
              statisticsManager ! IncreaseStatisticsMsg("c3.filesystem.check.root.fixed", 1)
              Some(accessManager.get(address))
            }
            case None => {
              log.warn("Failed to find correct root for domain " + domain)
              None
            }
          }

        } else {
          Some(resource)
        }

        correctRoot.foreach {
          r =>
            val node = Node.fromResource(r)
            if (node.isDirectory)
              checkDirectoryContents(node.asInstanceOf[Directory], domain)

        }
      }
      case None => {
        shouldStopFlag = true
      }
    }
  }

  override def progress: Int = {
    val totalRootsToCheck = fsRoots.size

    ((totalRootsToCheck - rootsToCheck.size).toFloat / totalRootsToCheck).toInt * 100
  }

  def checkDirectoryContents(directory: Directory, domain: String) {

    log debug "Checking directory " + directory.resource.address

    for (child <- directory.allChildren if !child.deleted) {

      val address = child.address

      try {
        statisticsManager ! IncreaseStatisticsMsg("c3.filesystem.check.total", 1)
        if (log.isTraceEnabled) {
          log trace "Checking child " + address
        }
        accessManager.get(address)
      } catch {
        case e: ResourceNotFoundException => {
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

    for (child <- directory.allChildren if !child.leaf && !child.deleted) {
      accessManager.getOption(child.address) match {
        case Some(resource) => {
          val node = Node.fromResource(resource)

          if (node.isDirectory)
            checkDirectoryContents(node.asInstanceOf[Directory], domain)
        }
        case None => {
          log.warn("Failed to load child of the directory " + directory.resource.address + " in the domain " + domain)
          directory.removeChild(child.name)
          accessManager.update(directory.resource)
        }
      }

    }
  }

  def findCorrectRoot(domainId: String): Option[String] = {

    val consumer = new GenericQueryConsumer[Option[String]] {

      var result: Option[String] = None

      def consume(resource: Resource) = {
        if (!resource.systemMetadata.has(Node.NODE_FIELD_NAME)) {
          log.info("Found correct root for domain " + resource.address)
          result = Some(resource.address)
          false
        } else {
          true
        }
      }

      def close() {}
    }

    queryManager.executeQuery(systemFields = Map("c3.domain.id" -> domainId, Node.NODE_FIELD_TYPE -> Node.NODE_TYPE_DIR), consumer = consumer)
  }


}