package org.aphreet.c3.platform.remote.rest.serialization

import com.thoughtworks.xstream.converters.{UnmarshallingContext, MarshallingContext, Converter}
import com.thoughtworks.xstream.io.{HierarchicalStreamReader, HierarchicalStreamWriter}
import java.lang.{Class => JClass}
import org.aphreet.c3.platform.resource.ResourceVersion
import org.joda.time.format.ISODateTimeFormat

class VersionConverter extends Converter{

  val dateFormat = ISODateTimeFormat.dateTime()

  override def canConvert(clazz:JClass[_]):Boolean = {
    clazz.equals(classOf[ResourceVersion])
  }

  override def marshal(value:java.lang.Object, writer:HierarchicalStreamWriter ,
                       context:MarshallingContext) {

    val version = value.asInstanceOf[ResourceVersion]

    writer.addAttribute("date", dateFormat.print(version.date.getTime))

    version.systemMetadata("c3.data.length") match {
      case Some(length) => writer.addAttribute("length", length)
      case None =>
    }

    version.systemMetadata("c3.data.md5") match {
      case Some(md5) => writer.addAttribute("hash", md5)
      case None =>
    }

  }

  override def unmarshal(reader:HierarchicalStreamReader, context:UnmarshallingContext):java.lang.Object = {
    throw new RuntimeException("This method is not implemented")
  }

}
