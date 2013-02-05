package org.aphreet.c3.platform.remote.rest.serialization

import com.thoughtworks.xstream.converters.{UnmarshallingContext, MarshallingContext, Converter}
import com.thoughtworks.xstream.io.{HierarchicalStreamReader, HierarchicalStreamWriter}
import java.lang.{Class => JClass}
import org.apache.commons.codec.binary.Base64
import org.aphreet.c3.platform.remote.rest.response.fs.FSNodeData
import org.joda.time.format.ISODateTimeFormat


class FSNodeDataConverter  extends Converter{

  val dateFormat = ISODateTimeFormat.dateTime()

  override def canConvert(clazz:JClass[_]):Boolean = {
    clazz.equals(classOf[FSNodeData])
  }

  override def marshal(value:java.lang.Object, writer:HierarchicalStreamWriter ,
                       context:MarshallingContext) {

    val data = value.asInstanceOf[FSNodeData]

    writer.addAttribute("date", dateFormat.print(data.date.getTime))
    writer.setValue(new String(Base64.encodeBase64(data.data, false)))
  }

  override def unmarshal(reader:HierarchicalStreamReader, context:UnmarshallingContext):java.lang.Object = {
    throw new RuntimeException("This method is not implemented")
  }
}
