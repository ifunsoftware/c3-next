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

import collection.Map
import collection.mutable

class Metadata(private val map: mutable.HashMap[String, String]) {

  def this() = this(new mutable.HashMap[String, String]())

  def asMap: Map[String, String] = map

  def put(key: String, value: String) = map.put(key, value)

  def get(key: String): Option[String] = map.get(key)

  def remove(key: String){
    map.remove(key)
  }

  def contains(key: String): Boolean = map.contains(key)

  def getList(key: String): Option[List[String]] = None

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
