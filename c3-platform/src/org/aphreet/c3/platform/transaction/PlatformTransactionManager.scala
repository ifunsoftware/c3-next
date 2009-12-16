package org.aphreet.c3.platform.transaction

object PlatformTransactionManager {

  val transactionHolder = new LocalTransactionHolder
  
  def currentTransaction:PlatformTransaction = transactionHolder.get
    
}

class LocalTransactionHolder extends ThreadLocal[PlatformTransaction] {
  
  override def initialValue = new PlatformTransaction
  
}