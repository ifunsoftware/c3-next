package org.aphreet.c3.platform.common

import org.apache.commons.logging.{LogFactory, Log}

trait Tracer {

  def log:Log

  def logOfClass(clazz: Class[_]):Log = LogFactory.getLog(clazz)

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

  def info(block: => String){
    if(log.isInfoEnabled){
      log.info(block)
    }
  }

  def warn(block: => String, e:Throwable = null){
    if(log.isWarnEnabled){
      log.warn(block, e)
    }
  }
}
