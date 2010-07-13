package org.aphreet.c3.platform.common.msg

import actors.Actor

case class RegisterListenerMsg(actor:Actor)
case class UnregisterListenerMsg(actor:Actor)