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

package org.aphreet.c3.platform.client.access.tools.worker

import java.util.concurrent.{TimeUnit, ArrayBlockingQueue}
import org.aphreet.c3.platform.client.access.http.C3HttpAccessor

class ConsumerWorker(val host:String, val user:String, val key:String, val queue:ArrayBlockingQueue[String]) extends Runnable{

  var done:Boolean = false
  var processed:Int = 0
  var errors:Int = 0
  var bytesRead:Long = 0l
  val client = new C3HttpAccessor(host, user, key)

  override def run(){

    var address = queue.poll(5, TimeUnit.SECONDS)

    while(address != null){
      try{
        val bytes = execute(address)
        bytesRead = bytesRead + bytes
        processed = processed + 1
      }catch{
        case e => {
          errors = errors + 1
          System.err.println("Error: " + e.getMessage)
        }
      }
      address = queue.poll(5, TimeUnit.SECONDS)
    }

    done = true
  }

  def execute(address:String):Long = {
    client.fakeRead(address) 
  }

  
}