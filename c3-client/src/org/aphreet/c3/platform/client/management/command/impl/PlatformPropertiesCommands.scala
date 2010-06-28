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
import org.aphreet.c3.platform.remote.api.management.Pair
import collection.immutable.TreeSet

object PlatformPropertiesCommands extends Commands {

  def instances = List(
      new SetPlatformPropertyCommand,
      new ListPlatformPropertiesCommand
    )
}


class SetPlatformPropertyCommand extends Command{

  def execute:String = {

    if(params.size < 2){
      "Not enought params\nUsage: set platform property <key> <value>"
    }else{
      management.setPlatformProperty(params.first, params.tail.first)
      "Property set"
    }

  }

  def name:List[String] = List("set", "platform", "property")
}

class ListPlatformPropertiesCommand extends Command{

  def execute:String = {
    val set = new TreeSet[Pair]

    (set ++ management.platformProperties).map(e => e.key + "=" + e.value + "\n").foldLeft("")(_ + _)

  }

  def name:List[String] = List("list", "platform", "properties")

  implicit def toOrdered(pair:Pair):Ordered[Pair] = {
    new OrderedPair(pair)
  }

  class OrderedPair(val pair:Pair) extends Ordered[Pair] {

    override def compare(that:Pair):Int = {
      that.key.compareTo(pair.key)
    }
  }
}