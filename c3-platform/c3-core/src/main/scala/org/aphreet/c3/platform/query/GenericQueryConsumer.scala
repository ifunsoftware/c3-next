package org.aphreet.c3.platform.query

import org.aphreet.c3.platform.resource.Resource

trait GenericQueryConsumer[T] {

  /**
   * @param resource to be consumed
   * @return value indicating if we need more resources
   */
  def consume(resource: Resource): Boolean

  /**
   * Closes consumer
   */
  def close()

  /**
   * @return result of the query
   */
  def result: T

}
