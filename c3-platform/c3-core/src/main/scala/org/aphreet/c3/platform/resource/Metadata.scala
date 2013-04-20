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

import collection.{Map, mutable}
import scala.collection.mutable.ArrayBuffer
import com.thoughtworks.xstream.XStream

class Metadata(private val map: mutable.HashMap[String, String]) {

  def this() = this(new mutable.HashMap[String, String]())

  def asMap: Map[String, String] = map

  @deprecated("use update instead", "1.3.6")
  def put(key: String, value: String) = map.put(key, value)

  @deprecated("use apply instead", "1.3.6")
  def get(key: String): Option[String] = map.get(key)

  def apply(key: String): Option[String] = map.get(key)

  def update(key: String, value: String) = map.put(key, value)

  def update(key: String, values: TraversableOnce[String]) = map.put(key, "[" + values.map(s => s.replaceAll(",", "\\,")).mkString(",") + "]")

  def update(key: String, value: Long) = map.put(key, value.toString)

  def update(key: String, value: Boolean) = map.put(key, value.toString)

  def remove(key: String){
    map.remove(key)
  }

  def has(key: String): Boolean = map.contains(key)

  def collectionValue(key: String): TraversableOnce[String] = {
    map.get(key) match {
      case None => None
      case Some(value) => MetadataHelper.parseSequence(value)
    }
  }


  def ++=(metadata: Metadata): Metadata = {
    map ++= metadata.asMap
    this
  }

  def ++(metadata: Metadata): Metadata = {
    this.clone() ++= metadata
  }

  def ++=(metadataMap: Map[String, String]): Metadata = {
    map ++= metadataMap
    this
  }

  override def clone() = {
    new Metadata(map.clone())
  }

  override def hashCode() = {
    map.hashCode()
  }

  override def equals(obj: Any): Boolean = {
    if(obj == null) return false

    if(!obj.isInstanceOf[Metadata]) return false

    obj.asInstanceOf[Metadata].map == map

  }
}

object MetadataHelper{

  private def isSequence(value: String): Boolean = {
    value.charAt(0) == '[' && value.charAt(value.length - 1) == ']'
  }

  //[key1:count1,key2:count2]
   //  val parts:Array[String] = tagInfo.split(":")
  def parseTagMap[T](values: String, converter: String => T): TraversableOnce[T] = {
    values match {
      case None => new ArrayBuffer[T]
      case Some(valueString) =>
        val result = new ArrayBuffer[T]
        parseSequence(valueString.toString).map(tagInfo =>
          result += converter(tagInfo))

        result
    }
  }

  def writeTagMap[T,B](values: Map[T,B], converter: (T,B) => String): String = {
      values match {
         case None => ""
         case Some(value) =>
          var result = ""
          values.map(tagInfo =>
             result += converter.apply(tagInfo._1, tagInfo._2))
          result
      }
  }

  def parseSequence(value: String): TraversableOnce[String] = {

    if (!isSequence(value)){
      Some(value)
    } else {

      var valueStart = 1

      val result = new ArrayBuffer[String]

      for (i <- valueStart until value.length - 1){
        if (value.charAt(i) == ',' && value.charAt(i - 1) != '\\'){
          result += value.substring(valueStart, i).replaceAll("\\\\,", ",")
          valueStart = i + 1
        }
      }

      result += value.substring(valueStart, value.length - 1)

      result
    }
  }




}
