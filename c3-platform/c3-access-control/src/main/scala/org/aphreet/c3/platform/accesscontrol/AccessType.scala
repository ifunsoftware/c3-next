package org.aphreet.c3.platform.accesscontrol

sealed trait AccessType

object LocalAccess extends AccessType
object RemoteAccess extends AccessType
