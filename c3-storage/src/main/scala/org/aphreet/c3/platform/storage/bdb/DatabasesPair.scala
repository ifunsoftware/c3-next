package org.aphreet.c3.platform.storage.bdb

import collection.mutable.HashMap
import com.sleepycat.je.{SecondaryDatabase, Database}

/**
 * Created by IntelliJ IDEA.
 * User: antey
 * Date: 03.05.11
 * Time: 15:16
 * To change this template use File | Settings | File Templates.
 */

class DatabasesPair  {
  var database : Database = null

  var secondaryDatabases = new HashMap[String, SecondaryDatabase]
}