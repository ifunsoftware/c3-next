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
package org.aphreet.c3.platform.remote.test.replication

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.remote.api.management.StorageDescription
import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.remote.replication.impl.config.{StorageSynchronizerException, StorageSynchronizer}

class StorageSynchronizerTestCase extends TestCase {

  def testStorageSync0 = {


    val remoteStorages = List(
      sd("a111", List("b222", "a333"), "PureBDBStorage", RW("")),
      sd("b111", List("b222", "b333"), "FileBDBStorage", RW(""))
    )

    val localStorages = List(
      st("c111", List("c222", "c333"), "PureBDBStorage"),
      st("d111", List("d222", "d333"), "FileBDBStorage")
    )

    val result = new StorageSynchronizer().getAdditionalIds(remoteStorages, localStorages)

    assertTrue(result.contains(("a111", "c333")))
    assertTrue(result.contains(("a111", "c222")))
    assertTrue(result.contains(("a111", "c111")))

    assertTrue(result.contains(("b111", "d333")))
    assertTrue(result.contains(("b111", "d222")))
    assertTrue(result.contains(("b111", "d111")))
  }

  def testStorageSync1 = {

    val remoteStorages = List(
      sd("a111", List("b222", "a333"), "PureBDBStorage", RW("")),
      sd("b111", List("b222", "b333"), "PureBDBStorage", RW(""))
    )

    val localStorages = List(
      st("c111", List("c222", "c333"), "PureBDBStorage"),
      st("d111", List("d222", "d333"), "FileBDBStorage")
    )

    try{
      val result = new StorageSynchronizer().getAdditionalIds(remoteStorages, localStorages)
      assertFalse(false)
    }catch{
      case e:StorageSynchronizerException => assertTrue(true)
    }

  }

  def testStorageSync2 = {


    val remoteStorages = List(
      sd("a111", List("b222", "a333"), "PureBDBStorage", RW("")),
      sd("b111", List("d111", "c111"), "FileBDBStorage", RW(""))
    )

    val localStorages = List(
      st("c111", List(), "PureBDBStorage"),
      st("d111", List(), "FileBDBStorage")
    )

    val result = new StorageSynchronizer().getAdditionalIds(remoteStorages, localStorages)

    assertTrue(result.isEmpty)
  }

  def testStorageSync3 = {


    val remoteStorages = List(
      sd("a111", List("b222", "a333"), "PureBDBStorage", RW("")),
      sd("e111", List("b222", "a333"), "PureBDBStorage", RW("")),
      sd("b111", List("b222", "b333"), "FileBDBStorage", RW(""))
    )

    val localStorages = List(
      st("c111", List("c222", "c333", "c444"), "PureBDBStorage"),
      st("d111", List("d222", "d333"), "FileBDBStorage")
    )

    val result = new StorageSynchronizer().getAdditionalIds(remoteStorages, localStorages)

    assertTrue(result.contains(("a111", "c333")))
    assertTrue(result.contains(("e111", "c222")))
    assertTrue(result.contains(("a111", "c111")))
    assertTrue(result.contains(("e111", "c444")))

    assertTrue(result.contains(("b111", "d333")))
    assertTrue(result.contains(("b111", "d222")))
    assertTrue(result.contains(("b111", "d111")))
  }

  def testAddId1 = {

    val localStorages = List(
      st("c111", List("c222", "c333", "c444"), "PureBDBStorage"),
      st("d111", List("d222", "d333"), "PureBDBStorage"),
      st("e111", List("d222"), "FileBDBStorage")
    )

    val primaryId = new StorageSynchronizer().getAdditionalId(localStorages, "faaa", "PureBDBStorage")

    assertEquals(Some("d111"), primaryId)

  }

  def testAddId2 = {

    val localStorages = List(
      st("c111", List("c222", "c333", "faaa"), "PureBDBStorage"),
      st("d111", List("d222", "d333"), "PureBDBStorage"),
      st("e111", List("d222"), "FileBDBStorage")
    )

    val primaryId = new StorageSynchronizer().getAdditionalId(localStorages, "faaa", "PureBDBStorage")

    assertEquals(None, primaryId)

  }

  def testAddId3 = {

    val localStorages = List(
      st("c111", List("c222", "c333"), "PureBDBStorage"),
      st("d111", List("d222", "d333"), "PureBDBStorage"),
      st("e111", List("d222", "faaa"), "FileBDBStorage")
    )

    val primaryId = new StorageSynchronizer().getAdditionalId(localStorages, "faaa", "PureBDBStorage")

    assertEquals(None, primaryId)

  }

  def testAddId4 = {

    val localStorages = List(
      st("c111", List("c222", "c333"), "PureBDBStorage"),
      st("d111", List("d222", "d333"), "PureBDBStorage"),
      st("e111", List("d222"), "PureBDBStorage")
    )

    try{
      val primaryId = new StorageSynchronizer().getAdditionalId(localStorages, "faaa", "FileBDBStorage")
      assertTrue("Exception must be here", false)
    }catch{
      case e:StorageSynchronizerException => assertTrue(true)
    }

  }


  def sd(id:String, ids:List[String], name:String, mode:StorageMode):StorageDescription = {
    val descr = new StorageDescription()
    descr.id = id
    descr.ids = ids.toArray
    descr.storageType = name
    descr.mode = mode.toString

    descr
  }

  def st(id:String, ids:List[String], name:String):Storage = {
    new StorageMock(id, ids, name)
  }
}

case class StorageMock(val mockId:String, val secIds:List[String], val stName:String) extends Storage{

  {
    this.ids = secIds
  }

  def id:String = mockId

  def add(resource:Resource):String = null

  def get(ra:String):Option[Resource] = null

  def update(resource:Resource):String = null

  def delete(ra:String) = {}

  def put(resource:Resource) = {}

  def appendSystemMetadata(ra:String, metadata:Map[String, String]) = {}

  def params:StorageParams = null
  def count:Long = 0

  def size:Long = 0

  def iterator(fields:Map[String,String],
               systemFields:Map[String,String],
               filter:Function1[Resource, Boolean]
          ):StorageIterator = null

  def close = {}

  def lock(ra:String) = {}

  def unlock(ra:String) = {}

  def path:Path = null

  def fullPath:Path = null

  def name:String = stName

  def createIndex(index:StorageIndex) = {}

  def removeIndex(index:StorageIndex) = {}
}