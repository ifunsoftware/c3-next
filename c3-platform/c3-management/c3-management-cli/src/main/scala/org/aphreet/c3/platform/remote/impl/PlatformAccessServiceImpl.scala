/**
 * Copyright (c) 2010, Mikhail Malygin
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

package org.aphreet.c3.platform.remote.impl

import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.remote.api.RemoteException
import org.aphreet.c3.platform.access.AccessManager
import org.aphreet.c3.platform.resource.{IdGenerator, ResourceAddress, ResourceException, ResourceSerializer}
import org.aphreet.c3.platform.storage.StorageManager
import org.aphreet.c3.platform.filesystem.{Directory, Node}
import javax.annotation.PostConstruct
import org.aphreet.c3.platform.common.Logger

@Component("platformAccessService")
class PlatformAccessServiceImpl extends PlatformAccessService{

  @Autowired
  var accessManager:AccessManager = _

  @Autowired
  var storageManager:StorageManager = _

  val logger = Logger(getClass)

  @PostConstruct
  def init(){
    logger.info("Starting platform access service")
  }

  def getResourceAsString(ra:String):String = {

    try{
      val resultBuilder = new StringBuilder

      try{
        val address = ResourceAddress(ra)

        resultBuilder.append("Resource info:\n")
        resultBuilder.append("\tCreate time: " + address.time + "\n")
        resultBuilder.append("\tRange prefix: " + IdGenerator.trailShort(address.randomPart) + "\n")

        resultBuilder.append("\nStorages: " + storageManager.storageForAddress(address).map(_.id).getOrElse("Can't find storage for resource")).append("\n\n")

      }catch{
        case e:ResourceException => resultBuilder.append(e.getMessage)
      }


      accessManager.getOption(ra) match {
        case Some(resource) => {
          resultBuilder.append(ResourceSerializer.toJSON(resource, full = true)).append("\n\n")

          if (Node.canBuildFromResource(resource)){

            Node.fromResource(resource) match {
              case d: Directory => resultBuilder.append(d.toJSON)
              case _ =>
            }
          }

        }
        case None => "Resource not found"
      }

      resultBuilder.toString()

    }catch{
      case e: Throwable => throw new RemoteException(e.getMessage)
    }
  }
}