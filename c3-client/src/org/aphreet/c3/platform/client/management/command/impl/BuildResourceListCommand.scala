package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.client.management.command.Command

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 9, 2010
 * Time: 12:26:18 AM
 * To change this template use File | Settings | File Templates.
 */

class BuildResourceListCommand extends Command{

  override def execute:String = {

    if(params.length > 0){
      management.buildResourceList(params.head)
      "Build started"
    }else{
      "Not enough params.\nUsage: build resource list <dir>"
    }
  }

  def name:List[String] = List("build", "resource", "list")
  
}