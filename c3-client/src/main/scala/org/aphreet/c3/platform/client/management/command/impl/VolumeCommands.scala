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

import org.aphreet.c3.platform.client.management.command.{Command, Commands}
import org.aphreet.c3.platform.remote.api.management.VolumeDescription

object VolumeCommands extends Commands{

  override def instances = List(
    new ListVolumesCommand
  )
}

class ListVolumesCommand extends Command {

  override def name:List[String] = List("list", "volumes")

  override def execute:String = {

    def format(desc:VolumeDescription):String =
      String.format("| %-31s | %8d | %13d | %13d | %13d |\n",
      desc.path,
      desc.storages,
      desc.size,
      desc.free,
      desc.available)

    val header = "|           Mount point           | Storages |     Total     |     Free      |   Available   |\n" +
                 "|---------------------------------|----------|---------------|---------------|---------------|\n"
    val footer = "|---------------------------------|----------|---------------|---------------|---------------|\n"

    val volumeList = management.volumes

    management.volumes.map(v => format(v)).foldLeft(header)(_ + _) + footer
  }
}