package org.aphreet.c3.platform.storage.query

import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 9, 2010
 * Time: 12:00:03 AM
 * To change this template use File | Settings | File Templates.
 */

trait QueryManager{

  def buildResourceList(dir:File)

}