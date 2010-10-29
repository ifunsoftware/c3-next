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

package org.aphreet.c3.platform.remote.replication.impl.data.queue

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import java.io.{FileWriter, BufferedWriter, File}
import org.aphreet.c3.platform.remote.replication.impl.data.{ReplicationDeleteEntry, ReplicationUpdateEntry, ReplicationAddEntry, ReplicationEntry}
import collection.mutable.HashMap

class ReplicationQueueSerializer(val directory:Path){

  def store(queue:HashMap[String, ReplicationEntry], host:ReplicationHost) = {

    if(queue.size > 0){

      directory.file.mkdirs

      val fileName = System.currentTimeMillis + "-" + host.systemId

      val file = new File(directory.file, fileName)

      val writer = new BufferedWriter(new FileWriter(file))

      try{

        for((key, entry) <- queue){
          writer.write(serializeEntry(entry))
          writer.write("\n")
        }

      }finally {
        writer.close
      }
    }
  }

  def serializeEntry(entry:ReplicationEntry):String = {
    val string = entry match {
      case ReplicationAddEntry(address) => address + " add"
      case ReplicationUpdateEntry(address, timestamp) => address + " update " + timestamp
      case ReplicationDeleteEntry(address) => address + " delete"
    }

    string
  }
}