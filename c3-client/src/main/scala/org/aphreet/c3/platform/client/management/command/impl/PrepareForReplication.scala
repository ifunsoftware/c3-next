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

package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.InteractiveCommand

import org.aphreet.c3.platform.remote.api.management.{PlatformManagementService, Pair}
import jline.ConsoleReader
import org.aphreet.c3.platform.management.cli.command.Commands

object PrepareForReplication extends Commands{

  def instances = List(new PrepareForReplication)

}

class PrepareForReplication extends InteractiveCommand {

  def getValue(array:Array[Pair], key:String):String = {
    array.filter(_.key == key).headOption match {
      case Some(value) => value.value
      case None => ""
    }
  }

  def setProperty(management:PlatformManagementService, key:String, value:String) {
    if(!value.isEmpty)
      //println("Setting " + key + " to " + value)
      management.setPlatformProperty(key, value)
  }

  override
  def execute(params:List[String], management:PlatformManagementService, reader:ConsoleReader):String = {

    val properties = management.platformProperties

    print("Public hostname [" + getValue(properties, "c3.public.hostname") + "]: ")
    val hostname = readInput(reader).trim

    print("HTTP port [" + getValue (properties, "c3.remote.http.port") + "]: ")
    val httpPort = readNumber(reader)

    print("HTTPS port [" + getValue (properties, "c3.remote.https.port") + "]: ")
    val httpsPort = readNumber(reader)

    print("Replication port [" + getValue (properties, "c3.remote.replication.port") + "]: ")
    val replicationPort = readNumber(reader)

    print("Replication queue path [" + getValue (properties, "c3.remote.replication.queue") + "]: ")
    val queue = readInput(reader).trim

    setProperty(management, "c3.public.hostname", hostname)
    setProperty(management, "c3.remote.http.port", httpPort)
    setProperty(management, "c3.remote.https.port", httpsPort)
    setProperty(management, "c3.remote.replication.port", replicationPort)
    setProperty(management, "c3.remote.replication.queue", queue)

    "Done"
  }

  def name:List[String] = List("set", "replication", "properties")
}
