package org.aphreet.c3.platform.transaction

trait ResourceTransaction {

  def commit
  
  def rollback
  
  def prepare
  
}
