package org.aphreet.c3.platform.client.command

import scala.collection.mutable.HashMap

import org.aphreet.c3.platform.management.rmi.PlatformRmiManagementService
import org.aphreet.c3.platform.access.rmi.PlatformRmiAccessService

class CommandFactory(val accessService:PlatformRmiAccessService, val managementService:PlatformRmiManagementService) {

  val root : CommandTreeNode = new CommandTreeNode(null)
  
  var commandList:List[Command] = List()
  
  { 
    register(new ListStorageCommand)
    register(new ListStorageTypesCommand)
    register(new AddStorageCommand)
    register(new SetStorageModeCommand)
    register(new GetPlatformPropertiesCommand)
    register(new SetPlatformPropertyCommand)
  }
  
  def getCommand(query:String):Option[Command] = {
    
    val input = List.fromArray(query.split("\\s+"))
    
    val classAndParams = root.classForInput(input)
    
    if(classAndParams._1 != null){
      val command = classAndParams._1.newInstance.asInstanceOf[Command]
      command.params = classAndParams._2
      command.access = accessService
      command.management = managementService
      Some(command)
    }else{
    	None
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