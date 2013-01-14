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

package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.storage.StorageIndex
import com.sleepycat.je.DatabaseEntry
import java.nio.ByteBuffer

class BDBStorageIndex(val storageIndex: StorageIndex) {

  def putSearchKey(key: String, entry: DatabaseEntry): Boolean = {
    if(BDBStorageIndex.isLongKey(storageIndex)){

      if (key.startsWith(">=")){
        putLong(entry, key.replaceFirst(">=", "").toLong)
        true
      }else{
        putLong(entry, key.toLong)
        false
      }
    }else{
      entry.setData(key.getBytes("UTF-8"))
      false
    }
  }

  private def putLong(entry: DatabaseEntry, long: Long){
    entry.setData(ByteBuffer.allocate(8).putLong(long).array())
  }
}

object BDBStorageIndex {

  implicit def indexToBDBIndex(index: StorageIndex): BDBStorageIndex = {
    new BDBStorageIndex(index)
  }

  def isLongKey(index: StorageIndex): Boolean = {
    index.system && !index.multi && (index.fields.head == "created" || index.fields.head == "updated")
  }

}
