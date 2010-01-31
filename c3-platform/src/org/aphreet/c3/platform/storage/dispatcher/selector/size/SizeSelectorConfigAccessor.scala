package org.aphreet.c3.platform.storage.dispatcher.selector.size

import java.io.File
import java.io.StringWriter

import org.aphreet.c3.platform.common.JSONFormatter
import org.aphreet.c3.platform.config.accessor.ConfigAccessor

import scala.collection.jcl.Set

import com.springsource.json.parser.{Node, MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl

import org.springframework.stereotype.Component

@Component
class SizeSelectorConfigAccessor extends SelectorConfigAccessor[Long]{

  override def filename = ""
  
  override def keyFromString(string:String):Long = string.toLong
  
  override def keyToString(key:Long):String = key.toString
  
}
