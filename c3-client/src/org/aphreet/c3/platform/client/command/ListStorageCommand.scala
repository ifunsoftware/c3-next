package org.aphreet.c3.platform.client.command

class ListStorageCommand extends Command {

  def execute:String = {
    val storages = management.listStorages
    
    val builder = new StringBuilder
    
    for(storage <- storages){
      
      builder.append(String.format("%-20s %4s %-7s %-10s %s\n", storage.storageType, storage.id, storage.contentType, storage.mode, storage.path))
      
    }
    
    builder.toString
    
  }
  
  def name = List("list", "storages")
}

