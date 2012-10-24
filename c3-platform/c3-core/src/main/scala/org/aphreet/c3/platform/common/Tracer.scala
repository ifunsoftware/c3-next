package org.aphreet.c3.platform.common

import org.apache.commons.logging.Log

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
