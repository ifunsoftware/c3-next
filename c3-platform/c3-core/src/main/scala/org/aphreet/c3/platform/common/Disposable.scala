package org.aphreet.c3.platform.common

import scala.language.reflectiveCalls
import scala.language.implicitConversions

trait Disposable[S] {

  def dispose()

}

object Disposable {
  implicit def closeToDisposable[S](what: S {def close()}):Disposable[S] = {
    new Disposable[S](){
      override def dispose(){
        try{
          what.close()
        }catch{
          case e: Throwable =>
        }
      }
    }
  }

  def using[S <% Disposable[S], T](what : S)(block : S => T) : T = {
    try {
      block(what)
    }
    finally {
      what.dispose
    }
  }
}
