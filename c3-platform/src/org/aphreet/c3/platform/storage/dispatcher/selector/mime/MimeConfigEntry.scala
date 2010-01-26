package org.aphreet.c3.platform.storage.dispatcher.selector.mime

case class MimeConfigEntry(val mimeType:String, val storage:String, val versioned:Boolean) {
 
  def isVersioned:Short = if(versioned) 1 else 0
}

