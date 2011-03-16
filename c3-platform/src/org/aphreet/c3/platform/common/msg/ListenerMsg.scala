package org.aphreet.c3.platform.common.msg

import actors.Actor

case class RegisterListenerMsg(actor:Actor)
case class UnregisterListenerMsg(actor:Actor)

case class RegisterNamedListenerMsg(actor:Actor, name:Symbol)
case class UnregisterNamedListenerMsg(actor:Actor, name:Symbol)
