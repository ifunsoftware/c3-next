/*
 * Copyright (c) 2011, Mikhail Malygin
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

package org.aphreet.c3.platform.mock

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource.{ResourceAddress, Resource}
import org.aphreet.c3.platform.storage._
import collection.mutable.HashMap

case class StorageMock(mockId:String, mockPath:String) extends Storage{

  def id:String = mockId

  def add(resource:Resource):String = ResourceAddress("12345678", "12345678", System.currentTimeMillis()).stringValue

  def get(ra:String):Option[Resource] = null

  def update(resource:Resource):String = resource.address

  def delete(ra:String) {}

  def put(resource:Resource) {}

  def appendSystemMetadata(ra:String, metadata:Map[String, String]) {}

  def params:StorageParams = StorageParams(mockId, path, "StorageMock", mode, List(), new HashMap[String, String])

  def count:Long = 0

  def size:Long = 0

  def iterator(fields:Map[String,String],
               systemFields:Map[String,String],
               filter:(Resource) => Boolean
          ):StorageIterator = null

  def close() {}

  def lock(ra:String) {}

  def unlock(ra:String) {}


  def path:Path = new Path(mockPath)

  def fullPath:Path = path

  def name:String = "StorageMock-" + mockId

  def createIndex(index:StorageIndex) {}

  def removeIndex(index:StorageIndex) {}
}