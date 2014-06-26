package org.aphreet.c3.platform.common

/**
 * Date:   6/26/14
 * Time:   3:20 PM
 */
trait CountingIterator[T] extends CloseableIterator[T] {

  def processedElements: Long

  def totalElements: Long

  def progress: Int = (processedElements * 100 / totalElements).toInt

}

class SimpleCountingIterator[T](val iterator: CloseableIterator[T], val totalElements: Long) extends CountingIterator[T] {

  private var elementsProcessed = 0l

  def processedElements: Long = elementsProcessed

  def hasNext: Boolean = iterator.hasNext

  def next(): T = {
    val e = iterator.next()
    elementsProcessed = elementsProcessed + 1
    e
  }

  def close() {
    iterator.close()
  }
}

