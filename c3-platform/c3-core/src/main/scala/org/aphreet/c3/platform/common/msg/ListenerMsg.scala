package org.aphreet.c3.platform.common.msg

import akka.actor.ActorRef

case class RegisterListenerMsg(actor:ActorRef)
case class UnregisterListenerMsg(actor:ActorRef)

case class RegisterNamedListenerMsg(actor:ActorRef, name:Symbol)
case class UnregisterNamedListenerMsg(actor:ActorRef, name:Symbol)
