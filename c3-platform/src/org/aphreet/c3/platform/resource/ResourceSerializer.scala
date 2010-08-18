package org.aphreet.c3.platform.resource

import org.aphreet.c3.platform.common.JSONFormatter
import com.springsource.json.writer.JSONWriterImpl
import java.io.StringWriter

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: Aug 18, 2010
 * Time: 7:37:47 PM
 * To change this template use File | Settings | File Templates.
 */

object ResourceSerializer {

  def toJSON(resource:Resource, full:Boolean = false) = {
    val swriter = new StringWriter()


    try{
      val writer = new JSONWriterImpl(swriter)

      writer.`object`

      writer.key("address").value(resource.address)

      writer.key("createDate").value(resource.createDate.getTime)


      writer.key("metadata")

      writer.`object`

      resource.metadata.foreach((e:(String, String)) => writer.key(e._1).value(e._2))

      writer.endObject

      if(full){
        writer.key("systemMetadata")

        writer.`object`

        resource.systemMetadata.foreach((e:(String, String)) => writer.key(e._1).value(e._2))

        writer.endObject
      }

      writer.key("versions")

      writer.array

      resource.versions.foreach(v => {
        writer.`object`

        writer.key("createDate").value(v.date.getTime)
        writer.key("dataLength").value(v.data.length)


        if(full){
          writer.key("revision").value(v.revision)

          writer.key("systemMetadata")

          writer.`object`

          v.systemMetadata.foreach((e:(String, String)) => writer.key(e._1).value(e._2))

          writer.endObject
        }
        writer.endObject
      })

      writer.endArray


      writer.endObject
      swriter.flush

      JSONFormatter.format(swriter.toString)

    }finally{
      swriter.close
    }
  }

}