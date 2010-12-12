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

package org.aphreet.c3.platform.client.management.command

import impl._

import scala.collection.mutable.HashMap
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

class CommandFactory(val accessService:PlatformAccessService, val managementService:PlatformManagementService) {

  val root : CommandTreeNode = new CommandTreeNode(null)

  {
    register(CommonCommands)
    register(MigrationCommands)
    register(PlatformPropertiesCommands)

    register(SizeMappingCommands)
    register(StorageCommands)
    register(TaskCommands)
    register(TypeMappingCommands)
    register(UserCommands)

    register(VolumeCommands)

    register(ReplicationCommands)
    
    register(new HelpCommand)
  }

  def getCommand(query:String):Option[Command] = {

    val input = query.trim.split("\\s+").toList

    val classAndParams = root.classForInput(input)

    if(classAndParams._1 != null){
      val command = classAndParams._1.newInstance.asInstanceOf[Command]
      command.params = classAndParams._2
      command.access = accessService
      command.management = managementService
      Some(command)
    }else{
      Some(new ErrorCommand("Command not found. Type help to get list of all commands"))
    }
  }

  def register(command:Commands){
    for(instance <- command.instances){
      register(instance)
    }
  }

  def register(command:Command) = {
    HelpCommand.addCommand(command)
    root.addCommand(command.name, command.getClass)
  }

}

class CommandTreeNode(val commandClass:Class[_]) {
    val map = new HashMap[String, CommandTreeNode]

    def classForInput(input:List[String]):(Class[_], List[String]) = {

      if(input.size > 0){
        map.get(input.head) match {
          case Some(node) => node.classForInput(input.tail)
          case None => {
            if(commandClass != null)
              (commandClass, input)
            else{
              val nonStrictNode = foundCommandAsSubstring(input.head)
              if(nonStrictNode != null){
                nonStrictNode.classForInput(input.tail)
              }else{
                (commandClass, input)
              }
            }
          }
        }
      }else{
        (commandClass, List())
      }
    }

    private def foundCommandAsSubstring(token:String):CommandTreeNode = {

      var foundNode:CommandTreeNode = null

      for((command, node) <- map){
        if(command.matches("^" + token + ".*")){
          if(foundNode == null){
            foundNode = node
          }else{
            return null
          }
        }
      }

      foundNode
    }

    def addCommand(input:List[String], commandClass:Class[_]):Unit = {
      if(input.size == 1){
        map.put(input.head, new CommandTreeNode(commandClass))
      }else{
        map.get(input.head) match {
          case Some(node) => node.addCommand(input.tail, commandClass)
          case None => {
            val node = new CommandTreeNode(null)
            map.put(input.head, node)
            node.addCommand(input.tail, commandClass)
          }
        }
      }
    }

  }
