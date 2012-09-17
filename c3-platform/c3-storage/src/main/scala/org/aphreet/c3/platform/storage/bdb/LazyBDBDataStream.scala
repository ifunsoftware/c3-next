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

package org.aphreet.c3.platform.storage.bdb

import com.sleepycat.je.{OperationStatus, LockMode, DatabaseEntry, Database}
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.resource._

class LazyBDBDataStream(val key: String, val database: Database) extends AbstractBytesDataStream {

  lazy val loadedBytes: Array[Byte] = fetchBytesFromDb

  private var loaded = false

  private def fetchBytesFromDb: Array[Byte] = {
    val valueEntry = new DatabaseEntry()

    val status = database.get(null, new DatabaseEntry(key.getBytes), valueEntry, LockMode.DEFAULT)

    loaded = true

    if (status == OperationStatus.SUCCESS)
      valueEntry.getData
    else
      throw new StorageException("Failed to load lazy data for key " + key + "; Operation status is " + status.toString)

  }

  override def loadBytes: Array[Byte] = loadedBytes

  override def copy:DataStream =
  {
    if(this.loaded)
      if(this.loadedBytes.length < 102400)
         return new BytesDataStream(this.loadedBytes)

    new LazyBDBDataStream(key, database)
  }
}