package org.aphreet.c3.platform.config

import java.util.EventObject

class PropertyChangeEvent(val name:String, val oldValue:String, val newValue:String, val src:java.lang.Object) extends EventObject(src)
