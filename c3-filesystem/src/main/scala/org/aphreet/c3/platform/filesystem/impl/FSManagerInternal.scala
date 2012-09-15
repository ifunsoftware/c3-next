package org.aphreet.c3.platform.filesystem.impl

import org.aphreet.c3.platform.filesystem.FSDirectoryTask

/**
 * Created with IntelliJ IDEA.
 * User: aphreet
 * Date: 9/15/12
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */

trait FSManagerInternal {

  def executeDirectoryTasks(address:String, tasks:List[FSDirectoryTask])

}

