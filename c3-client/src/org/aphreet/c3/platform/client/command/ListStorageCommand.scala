package org.aphreet.c3.platform.client.command

class ListStorageCommand extends Command {

  def execute:String = {
    val storages = management.listStorages
    
    val builder = new StringBuilder
    
    for(storage <- storages){
      
      builder.append(String.format("%-20s %4s %-10s %6d %s\n", storage.storageType, storage.id, storage.mode, storage.count, storage.path))
      
    }
    
    builder.toString
    
  }
  
  def name = List("list", "storages")
}

