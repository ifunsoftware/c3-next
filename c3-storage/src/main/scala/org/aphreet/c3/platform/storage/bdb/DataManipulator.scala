package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.resource.Resource
import com.sleepycat.je.Transaction

trait  DataManipulator {

  def loadData(resource:Resource)

  protected def storeData(resource:Resource, tx:Transaction):Unit = storeData(resource)

  protected def storeData(resource:Resource):Unit = {}

  protected def deleteData(ra:String, tx:Transaction):Unit = deleteData(ra)

  protected def deleteData(ra:String):Unit = {}

  protected def putData(resource:Resource):Unit = {}

  protected def putData(resource:Resource, tx:Transaction):Unit = putData(resource)
  
}