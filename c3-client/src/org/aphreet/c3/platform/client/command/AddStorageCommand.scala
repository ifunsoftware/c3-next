package org.aphreet.c3.platform.client.command

class AddStorageCommand extends Command{

  def execute:String = {
    if(params.size < 2){
      "Not enought params.\nUsage: create storage <type> <path>"
    }else{
      management.createStorage(params.first, params.tail.first)
      "Storage created"
    }
  }
  
  def name = List("create", "storage")
}
