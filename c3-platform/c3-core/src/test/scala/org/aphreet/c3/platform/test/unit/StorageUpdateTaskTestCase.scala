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

package org.aphreet.c3.platform.test.unit

import junit.framework.TestCase
import org.easymock.EasyMock._
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.{RW, StorageMode, StorageIterator}
import org.aphreet.c3.platform.mock.StorageMock
import org.aphreet.c3.platform.storage.updater.Transformation
import org.aphreet.c3.platform.storage.updater.impl.StorageUpdateTask

class StorageUpdateTaskTestCase extends TestCase{

  def testStorageUpdateTask(){

    val resources = Array(new Resource, new Resource, new Resource)

    val mockedIterator = new StorageIterator{

      var closed = false

      val iterator = resources.iterator

      def hasNext = iterator.hasNext

      def next() = iterator.next()

      def close() {
        closed = true
      }

      def objectsProcessed:Int = 0
    }

    val storage = new StorageMock("1", ""){

      override def mode:StorageMode = RW("")

      override def iterator(md:Map[String, String], smd:Map[String, String], filter:(Resource) => Boolean):StorageIterator
      = mockedIterator
    }

    val transofrmation = createMock(classOf[Transformation])
    expect(transofrmation.apply(resources(0)))
    expect(transofrmation.apply(resources(1)))
    expect(transofrmation.apply(resources(2)))

    replay(transofrmation)

    val task = new StorageUpdateTask(List(storage), List(transofrmation))
    task.run()

    verify(transofrmation)

  }
}
