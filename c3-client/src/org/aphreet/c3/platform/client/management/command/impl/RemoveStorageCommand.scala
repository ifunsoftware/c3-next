package org.aphreet.c3.platform.client.management.command.impl

class RemoveStorageCommand extends Command{

  def execute:String = {
    if(params.size < 1){
      "Not enought params.\nUsage: remove storage <storage id>"
    }else{
      management.removeStorage(params.first)
      "Storage removed"
    }
  }
  
  def name = List("remove", "storage")
  
}
