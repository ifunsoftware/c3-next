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
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.remote.replication.impl.data.queue.{ReplicationQueueStorageImpl, ReplicationQueueStorage}
import java.io.File
import org.aphreet.c3.platform.remote.replication.impl.data._

class ReplicationQueueStorageTestCase extends TestCase{

  var testDir:File = null

  override def setUp(){
    testDir = new File(System.getProperty("user.home"), "c3_int_test")
    testDir.mkdirs
  }

  override def tearDown(){
    def delDir(directory:File) {
      if(directory.isDirectory) directory.listFiles.foreach(delDir(_))
      directory.delete
    }
    delDir(testDir)
  }

  def testStorage() {

    val storage = createStorage("1111")

    val tasks = Set(ReplicationTask("1228249156", "0e6315ea-c2fd-4bef-936e-59cef7943841-6a47", AddAction),
      ReplicationTask("1228249156", "0e6315ea-c2fd-4bef-936e-59cef7943841-6a48", AddAction),
      ReplicationTask("1228249156", "0e6315ea-c2fd-4bef-936e-59cef7943841-6a49", DeleteAction),
      ReplicationTask("1228249156", "0e6315ea-c2fd-4bef-936e-59cef7943841-6a40", AddAction),
      ReplicationTask("1228249156", "0e6315ea-c2fd-4bef-936e-59cef7943841-6a41", DeleteAction),
      ReplicationTask("1228249156", "0e6315ea-c2fd-4bef-936e-59cef7943841-6a42", UpdateAction(1l)))

    storage.add(tasks)

    //Small test to check behavior with duplicate entries
    storage.add(Set(ReplicationTask("1228249156", "0e6315ea-c2fd-4bef-936e-59cef7943841-6a40", AddAction)))

    val iterator = storage.iterator

    while(iterator.hasNext){

      val task = iterator.next()
      println("Enumerating: " + task)

      assertTrue(tasks.contains(task))
    }

    iterator.close()

    val iterator2 = storage.iterator

    while(iterator2.hasNext){
      val task = iterator2.next()

      println("Deleting:" + task)
      iterator2.remove()
    }

    iterator2.close()

    val iterator3 = storage.iterator

    var taskCount = 0

    while(iterator3.hasNext){
      println(iterator3.next())
      taskCount = taskCount + 1
    }

    iterator3.close()

    assertEquals(0, taskCount)
    //var tasksToCheck = tasks.clone

  }


  def createStorage(dir:String):ReplicationQueueStorage = {
    new ReplicationQueueStorageImpl(new Path(new File(testDir, dir)))
  }

}