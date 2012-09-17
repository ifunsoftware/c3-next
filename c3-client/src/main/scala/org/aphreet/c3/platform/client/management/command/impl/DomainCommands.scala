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

package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.{Command, Commands}
import org.aphreet.c3.platform.remote.api.management.{PlatformManagementService, DomainDescription}

object DomainCommands  extends Commands{

  def instances = List(
      new CreateDomainCommand,
      new UpdateDomainNameCommand,
      new ResetDomainKeyCommand,
      new ListDomainsCommand,
      new SetDomainModeCommand,
      new ShowDomainCommand
  )
}

class CreateDomainCommand extends Command {

  override
  def execute(params:List[String], management:PlatformManagementService):String = {

    if (params.size < 1) {
      wrongParameters("create domain <name>")
    } else {

      management.createDomain(params.head)
      "Domain created"
    }
  }

  def name = List("create", "domain")
}

class UpdateDomainNameCommand extends Command {

  override
  def execute(params:List[String], management:PlatformManagementService):String = {

    if(params.size < 2){
      wrongParameters("update domain set name <old name> <new name>")
    }else{
      management.updateDomainName(params.head, params.tail.head)
      "Domain name updated"
    }
  }

  def name = List("update", "domain", "set", "name")
}

class ResetDomainKeyCommand extends Command {

  override
  def execute(params:List[String], management:PlatformManagementService):String = {

    if(params.size < 1){
      wrongParameters("update domain reset key <name>")
    }else{
      val key = management.generateDomainKey(params.head)
      "Domain key reset. New key is: " + key
    }
  }

  def name = List("update", "domain", "reset", "key")

}

class SetDomainModeCommand extends Command {

  override
  def execute(params:List[String], management:PlatformManagementService):String = {

    if(params.size < 2){
      wrongParameters("update domain set mode <name> <available|readonly|disabled>")
    }else{
      management.setDomainMode(params.head, params.tail.head)
      "Domain mode set"
    }
  }

  def name = List("update", "domain", "set", "mode")

}

class ListDomainsCommand extends Command {


  def format(desc:DomainDescription):String =
    String.format("| %-20s | %-10s |\n",
      desc.name,
      desc.mode)

  val header = "|         Name         |    Mode    |\n" +
               "|----------------------|------------|\n"
  val footer = "|----------------------|------------|\n"

  override
  def execute(management:PlatformManagementService):String =
    management.listDomains.map(d => format(d)).foldLeft(header)(_ + _) + footer


  def name = List ("list", "domains")
}

class ShowDomainCommand extends Command {

  override
  def execute(params:List[String], management:PlatformManagementService):String = {
    if(params.length < 1){
      wrongParameters("show domain <name>")
    }else{
      val domains = management.listDomains.filter(_.name == params.head)

      if(domains.size > 0){
        val domain = domains(0)

        "Domain:\n" +
                "Id  : " + domain.id + "\n" +
                "Name: " + domain.name + "\n" +
                "Mode: " + domain.mode + "\n" +
                "Key : " + domain.key + "\n"

      }else{
        "Domain with name \"" + params.head + "\" is not found"
      }
    }
  }

  def name = List("show", "domain")
}