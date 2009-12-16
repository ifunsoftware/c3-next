package org.aphreet.c3.platform.storage.common

import org.aphreet.c3.platform.transaction.ResourceTransaction
import com.sleepycat.je.Transaction

class BDBTransaction(val tx:Transaction) extends ResourceTransaction{

  def commit = tx.commit
  
  def rollback = tx.abort
  
  def prepare = {
    if(!tx.getPrepared){
      throw new StorageException("BDB is not prepared")
    }
  }
}
