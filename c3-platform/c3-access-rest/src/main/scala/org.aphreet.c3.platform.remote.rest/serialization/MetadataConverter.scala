package org.aphreet.c3.platform.remote.rest.serialization

import com.thoughtworks.xstream.converters.{UnmarshallingContext, MarshallingContext, Converter}
import java.lang.Class
import com.thoughtworks.xstream.io.{HierarchicalStreamReader, HierarchicalStreamWriter}
import org.aphreet.c3.platform.resource.Metadata

class MetadataConverter extends Converter{


  override def canConvert(clazz:Class[_]):Boolean = {
    clazz == classOf[Metadata]
  }


  override def marshal(value:java.lang.Object, writer:HierarchicalStreamWriter ,
                       context:MarshallingContext) {
    val map = value.asInstanceOf[Metadata].asMap

    for((k, v) <- map){
      writer.startNode("element")
      writer.addAttribute("key", k)
      writer.startNode("value")
      writer.setValue(v)
      writer.endNode()
      writer.endNode()
    }
  }

  override def unmarshal(reader:HierarchicalStreamReader, context:UnmarshallingContext):java.lang.Object = {
    throw new RuntimeException("This method is not implemented")
  }
}