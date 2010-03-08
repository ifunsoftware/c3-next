package org.aphreet.c3.platform.storage.query.impl

import org.aphreet.c3.platform.task.Task
import org.aphreet.c3.platform.storage.{StorageIterator, Storage}
import java.io.{FileWriter, BufferedWriter, File}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 9, 2010
 * Time: 12:00:40 AM
 * To change this template use File | Settings | File Templates.
 */

class ResourceListTask(val storage:Storage, val file:File) extends Task{

  var iterator:StorageIterator = _
  var fileWriter:BufferedWriter = _

  override def preStart = {
    iterator = storage.iterator
    fileWriter = new BufferedWriter(new FileWriter(file))
  }

  override def step = {
    val resource = iterator.next
    fileWriter.write(resource.address + "\n")
  }

  override def postComplete = {
    iterator.close
    iterator = null
    fileWriter.close
    fileWriter = null
  }

  override def postFailure = {
    try{
      iterator.close
      iterator = null
      fileWriter.close
      fileWriter = null
    }catch{
      case e=> log error e
    }
  }

  override def shouldStop:Boolean = !iterator.hasNext

  override def progress:Int = {
    if(iterator != null){
      val toProcess:Float = iterator.objectsProcessed
      val total:Float = storage.count

      if(total > 0){
        val overalProgress = toProcess * 100 /total
        overalProgress.intValue
      }else{
        0
      }
     }else -1
  }

  override def finalize = {
    if(iterator != null)
      try{
        iterator.close
      }catch{
        case e => e.printStackTrace
      }
    if(fileWriter != null){
      try{
        fileWriter.close
      }catch{
        case e=> e.printStackTrace
      }
    }
  }
}