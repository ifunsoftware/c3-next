package org.aphreet.c3.platform.storage.dispatcher.selector.mime

import java.io.File
import java.io.StringWriter

import scala.collection.Map
import scala.collection.mutable.HashMap
import scala.collection.jcl.Set

import org.aphreet.c3.platform.common.{Path, JSONFormatter}
import org.aphreet.c3.platform.config.accessor.ConfigAccessor

import com.springsource.json.parser.{Node, MapNode, ListNode, AntlrJSONParser, ScalarNode}
import com.springsource.json.writer.JSONWriterImpl;

import org.springframework.stereotype.Component

@Component
class MimeTypeConfigAccessor extends SelectorConfigAccessor[String]{

  override def filename = "c3-mime-types.json"
  
  override def keyFromString(string:String):String = string
  
  override def keyToString(key:String):String = key
  
}
