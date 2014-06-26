package org.aphreet.c3.platform.common

import scala.collection.Iterable
import scala.collection.Iterator

trait CloseableIterator[T] extends Iterator[T] {

  def close()

}

trait CloseableIterable[T] extends Iterable[T] {

  override def iterator: CloseableIterator[T]

}

class SimpleCloseableIterable[T](val iterable: Iterable[T]) extends CloseableIterable[T] {

  override def iterator: CloseableIterator[T] = new CloseableIterator[T] {

    val internalIterator = iterable.iterator

    def next() = internalIterator.next()

    def hasNext = internalIterator.hasNext

    def close() {}
  }
}

object CloseableIterable {

  def apply[T](iterable: Iterable[T]): CloseableIterable[T] = {
    new SimpleCloseableIterable[T](iterable)
  }

}
