/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.resource

import org.aphreet.c3.platform.common.JSONFormatter
import com.springsource.json.writer.JSONWriterImpl
import java.io.StringWriter


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
      swriter.flush()

      JSONFormatter.format(swriter.toString)

    }finally{
      swriter.close()
    }
  }

}