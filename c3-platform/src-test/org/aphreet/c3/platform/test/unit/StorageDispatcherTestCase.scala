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
package org.aphreet.c3.platform.test.unit

import junit.framework.Assert._
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage._
import collection.mutable.HashMap
import dispatcher.impl.DefaultStorageDispatcher
import junit.framework.{AssertionFailedError, TestCase}

class StorageDispatcherTestCase extends TestCase{


  def testRandom{

    val storageDispatcher = new DefaultStorageDispatcher

    val storages = List(new StorageStub0("1"), new StorageStub0("2"), new StorageStub0("3"))

    val map : HashMap[String, Int] = new HashMap;

    map.put("1", 0)
    map.put("2", 0)
    map.put("3", 0)


    for(i <- 1 to 10000){

      val id = storageDispatcher.random(storages).id

      val num:Int = map.get(id).get

      map.put(id, (num+1))
    }
    
    val baseline = 10000/3f

    try{
      for((key, value) <- map){
        assertTrue(math.abs(value - baseline)/baseline < 0.1f)
      }
    }catch{
      case e:AssertionFailedError =>
        println("Average calls per storage")
        println(map)
        throw e;
    }
  }


}

class StorageStub0(storageId:String) extends Storage{

  {
    this.mode = RW("")
  }

  def appendSystemMetadata(ra:String, metadata:Map[String, String]) = {}

  def id:String = storageId

  def add(resource:Resource):String = ""

  def get(ra:String):Option[Resource] = None

  def update(resource:Resource):String = ""

  def delete(ra:String) = {}

  def put(resource:Resource) = {}

  def lock(ra:String) = {}

  def unlock(ra:String) = {}

  def params:StorageParams = null

  def count:Long = 0

  def size:Long = 0

  def iterator(fields:Map[String,String],
               systemFields:Map[String,String],
               filter:Function1[Resource, Boolean]
          ):StorageIterator = null

  def close = {}


  def path:Path = null

  def fullPath:Path = null

  def name:String = ""

  def createIndex(index:StorageIndex) = {}

  def removeIndex(index:StorageIndex) = {}
  
}