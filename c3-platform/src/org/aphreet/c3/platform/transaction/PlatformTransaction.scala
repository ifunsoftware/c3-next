package org.aphreet.c3.platform.transaction

import scala.collection.mutable.HashSet
import org.aphreet.c3.platform.exception.TransactionException

class PlatformTransaction {

  var transactions = new HashSet[ResourceTransaction]
  
  private var state:TransactionState = NEW
  
  def addResourceTx(tx:ResourceTransaction) = {
    
    state match {
      case NEW => transactions + tx
      case _ => {
        throw new TransactionException("Can't add resource because tx state is not NEW" )
      }
    }
    
  }
  
  def commit = 
    try{
      state match {
        case COMMITTED  => throw new TransactionException("Transaction already committed")
        case ROLLEDBACK => throw new TransactionException("Can't commit transaction due to its state:ROLLEDBACK")
        case ROLLBACK_ONLY => throw new TransactionException("Tx can only be rolled back")
        case _ => {
          transactions.foreach(_.commit)
          state = COMMITTED
        }
      }
      
    }catch{
      case e => {
        state = ROLLBACK_ONLY
        throw e;
      }
    }
  
  
  def prepare = 
	try{
	  state match {
        case NEW => {
          transactions.foreach(_.prepare)
          state = PREPARED
        }
        case _ => throw new TransactionException("Can't commit transaction due to its state: " + state.name)
      }
	}catch{
	  case e=> {
	    state = ROLLBACK_ONLY
	    throw e
	  }
	}
  
  
  def rollback = 
    try{
	  state match {
        
	    case COMMITTED  => throw new TransactionException("Transaction already committed")
        case _ => {
          transactions.foreach(_.rollback)
          state = ROLLEDBACK
        }
      }
	}catch{
	  case e=> {
	    state = ROLLBACK_ONLY
	    throw e
	  }
	}
  
}

sealed class TransactionState(val name:String)

object COMMITTED extends TransactionState("Committed")
object ROLLEDBACK extends TransactionState("Rolledback")
object PREPARED extends TransactionState("Prepared")
object ROLLBACK_ONLY extends TransactionState("Rollback_only")
object NEW extends TransactionState("New")
