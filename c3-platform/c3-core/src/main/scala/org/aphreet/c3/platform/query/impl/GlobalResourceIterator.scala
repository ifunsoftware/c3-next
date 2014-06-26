package org.aphreet.c3.platform.query.impl

import org.aphreet.c3.platform.common.{CountingIterator, CloseableIterator}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.{StorageIterator, Storage}

/**
 * Date:   6/25/14
 * Time:   3:18 PM
 */
class GlobalResourceIterator(val storages: List[Storage],
                             val fields: Map[String, String],
                             val sysFields: Map[String, String]) extends CountingIterator[Resource] {

  private var storagesToProcess: List[Storage] = storages

  private var iterator: Option[StorageIterator] = None

  private var currentResource: Resource = fetchNext()

  private var objectsProcessed: Int = 0

  private val totalObjectsToProcess = storages.foldLeft(0l)(_ + _.count)

  private def fetchNext(): Resource = {
    val iterator = getIterator

    if (iterator == null) {
      null
    } else {
      if (iterator.hasNext) {
        iterator.next()
      } else {
        discardIterator()
        fetchNext()
      }
    }
  }

  private def getIterator: StorageIterator = {
    iterator match {
      case Some(it) => it
      case None => {
        if (storagesToProcess.isEmpty) {
          null
        } else {
          iterator = Some(storagesToProcess.head.iterator(fields, sysFields))
          storagesToProcess = storagesToProcess.tail
          iterator.get
        }
      }
    }
  }

  private def discardIterator() {
    objectsProcessed = objectsProcessed + iterator.map(_.objectsProcessed).getOrElse(0)
    iterator.map(_.close())
    iterator = None
  }


  def hasNext: Boolean = currentResource != null

  def next(): Resource = {
    val result = currentResource
    currentResource = fetchNext()
    result
  }

  def close(): Unit = {
    discardIterator()
  }

  def processedElements = {
    objectsProcessed + iterator.map(_.objectsProcessed).getOrElse(0)
  }

  def totalElements = totalObjectsToProcess
}