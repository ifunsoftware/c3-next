package org.aphreet.c3.platform.client.command

class StartMigrationCommand extends Command{

  def execute:String = {
    if(params.size < 2){
      "Not enought params.\nUsage: start migration <source id> <target id>"
    }else{
      management.migrate(params.first, params.tail.first)
      "Migration started"
    }
  }
  
  def name = List("start", "migration")
}
