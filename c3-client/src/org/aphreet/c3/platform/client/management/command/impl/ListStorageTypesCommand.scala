package org.aphreet.c3.platform.client.management.command.impl

class ListStorageTypesCommand extends Command{

  def execute:String = {
    val types = management.listStorageTypes
    
    val builder = new StringBuilder
    
    for(tp <- types){
      builder.append(tp).append("\n")
    }
    
    builder.toString
    
  }
  
  def name = List("list", "storage", "types")
}