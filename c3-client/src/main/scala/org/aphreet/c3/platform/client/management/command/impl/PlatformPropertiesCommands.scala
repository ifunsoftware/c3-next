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
import org.aphreet.c3.platform.remote.api.management.{PlatformManagementService, Pair}
import collection.immutable.TreeSet

object PlatformPropertiesCommands extends Commands {

  def instances = List(
      new SetPlatformPropertyCommand,
      new ListPlatformPropertiesCommand,
      new ListStatisticsCommand
    )
}


class SetPlatformPropertyCommand extends Command{

  override
  def execute(params:List[String], management:PlatformManagementService):String = {

    if(params.size < 2){
      "Not enought params\nUsage: set platform property <key> <value>"
    }else{
      management.setPlatformProperty(params.head, params.tail.head)
      "Property set"
    }

  }

  def name:List[String] = List("set", "platform", "property")
}

class ListPlatformPropertiesCommand extends Command{

  val header = "|                    Key                     |                        Value                       |\n"+
               "|--------------------------------------------|----------------------------------------------------|\n"

  val footer = "|--------------------------------------------|----------------------------------------------------|\n"

  override
  def execute(management:PlatformManagementService):String = {
    val set = new TreeSet[Pair]()(
      new Ordering[Pair] {
        override def compare(x:Pair, y:Pair):Int = x.key.compareTo(y.key)
      }
    )

    (set ++ management.platformProperties)
      .map(e => String.format("| %-42s | %-50s |\n", e.key, e.value)).foldLeft(header)(_ + _) + footer

  }

  def name:List[String] = List("list", "platform", "properties")
  
}

class ListStatisticsCommand extends Command{

  val header = "|                    Key                   |      Value      |\n" +
               "|------------------------------------------|-----------------|\n"

  val footer = "|------------------------------------------|-----------------|\n"

  override
  def execute(management:PlatformManagementService):String = {

    management.statistics
      .map(e => (String.format("| %-40s | %15s |\n", e.key, e.value))).foldLeft(header)(_ + _) + footer
    
  }

  def name = List("show", "statistics")
}
