package org.aphreet.c3.platform.accesscontrol

/**
 * Copyright iFunSoftware 2011
 * @author Mikhail Malygin
 */

sealed trait Action {

}

object READ extends Action
object CREATE extends Action
object UPDATE extends Action
object DELETE extends Action

