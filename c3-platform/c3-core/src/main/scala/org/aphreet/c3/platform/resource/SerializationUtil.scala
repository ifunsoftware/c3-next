/*
 * Copyright (c) 2013, Mikhail Malygin
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
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
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

import java.io.{DataInputStream, DataOutputStream}
import scala.Predef.String
import collection.mutable

object SerializationUtil {

  val MD_ENCODING = "UTF-8"

  def writeString(value: String, os: DataOutputStream) {
    var string:String = ""

    if(value != null) string = value

    val bytes = string.getBytes(Resource.MD_ENCODING)
    os.writeInt(bytes.length)
    os.write(bytes)
  }

  def readString(is: DataInputStream): String = {
    val strSize = is.readInt
    val strArray = new Array[Byte](strSize)
    is.read(strArray)

    new String(strArray, MD_ENCODING)
  }

  def writeMetadata(metadata: Metadata, os: DataOutputStream) {
    os.writeInt(metadata.asMap.size)

    for(key <- metadata.asMap.keySet){
      writeString(key, os)
      writeString(metadata.asMap(key), os)
    }
  }

  def readMetadata(is: DataInputStream): Metadata = {
    val map = new mutable.HashMap[String, String]()

    val mapSize: Int = is.readInt

    new Range.Inclusive(1, mapSize, 1).foreach(i =>
      map.put(readString(is), readString(is))
    )

    new Metadata(map)
  }

}
