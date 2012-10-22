package org.aphreet.c3.platform.common

import org.apache.commons.logging.Log

/**
 * Created with IntelliJ IDEA.
 * User: aphreet
 * Date: 10/23/12
 * Time: 01:45
 * To change this template use File | Settings | File Templates.
 */
trait Tracer {

  def log:Log

  def trace(block: => String){
    if(log.isTraceEnabled){
      log.trace(block)
    }
  }

  def debug(block: => String){
    if(log.isDebugEnabled){
      log.debug(block)
    }
  }


}
