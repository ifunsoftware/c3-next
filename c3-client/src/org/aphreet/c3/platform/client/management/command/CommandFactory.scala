package org.aphreet.c3.platform.client.management.command

import impl._

import scala.collection.mutable.HashMap
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

class CommandFactory(val accessService:PlatformAccessService, val managementService:PlatformManagementService) {

  val root : CommandTreeNode = new CommandTreeNode(null)
  
  var commandList:List[Command] = List()
  
  { 
    register(new AddSizeMappingCommand)
    register(new AddTypeMappingCommand)
    register(new AddStorageCommand)
    register(new HelpCommand)
    register(new GetPlatformPropertiesCommand)
    register(new ListSizeMappingCommand)
    register(new ListStorageCommand)
    register(new ListStorageTypesCommand)
    register(new ListTasksCommand)
    register(new ListTypeMappingCommand)
    register(new PauseTaskCommand)
    register(new RemoveSizeMappingCommand)
    register(new RemoveStorageCommand)
    register(new RemoveTypeMappingCommand)
    register(new ResumeTaskCommand)
    register(new SetPlatformPropertyCommand)
    register(new SetStorageModeCommand)
    register(new ShowResourceCommand)
    register(new StartMigrationCommand)
    register(new QuitCommand)
    register(new EmptyCommand)
    
    HelpCommand.commandList = commandList
  }
  
  def getCommand(query:String):Option[Command] = {
    
    val input = List.fromArray(query.trim.split("\\s+"))
    
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
  
  def register(command:Command) = {
    commandList = commandList ::: List(command)
    root.addCommand(command.name, command.getClass)
  }
  
  
  
  class CommandTreeNode(val commandClass:Class[_]) {
    val map = new HashMap[String, CommandTreeNode]
    
    def classForInput(input:List[String]):(Class[_], List[String]) = {
      
      if(input.size > 0){
    	map.get(input.first) match {
      	  case Some(node) => node.classForInput(input.tail)
          case None => (commandClass, input)
      	}
      }else{
        (commandClass, List())
      }
    }
    
    def addCommand(input:List[String], commandClass:Class[_]):Unit = {
      if(input.size == 1){
        map.put(input.first, new CommandTreeNode(commandClass))
      }else{
        map.get(input.first) match {
          case Some(node) => node.addCommand(input.tail, commandClass)
          case None => {
            val node = new CommandTreeNode(null)
            map.put(input.first, node)
            node.addCommand(input.tail, commandClass)
          }
        }
      }
    }
    
  }
  
}
