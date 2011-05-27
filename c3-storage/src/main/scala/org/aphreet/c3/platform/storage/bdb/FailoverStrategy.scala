package org.aphreet.c3.platform.storage.bdb

/*
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: 5/25/11
 * Time: 2:34 AM
 */
trait FailoverStrategy {

  protected def failuresArePossible(block: => Any):Unit

}