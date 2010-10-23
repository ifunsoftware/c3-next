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
