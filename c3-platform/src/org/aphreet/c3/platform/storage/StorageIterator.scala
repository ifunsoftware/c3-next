package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.resource.Resource

trait StorageIterator extends java.util.Iterator[Resource]{

  def close
  
  def remove = {
    throw new UnsupportedOperationException
  }
}
