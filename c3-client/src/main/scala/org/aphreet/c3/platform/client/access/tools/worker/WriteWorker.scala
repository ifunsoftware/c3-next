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

import java.util.Random
import java.util.concurrent.LinkedBlockingQueue
import org.aphreet.c3.platform.client.access.http.C3HttpAccessor

class WriteWorker(val number: Int,
                  val host: String,
                  val user: String,
                  val key: String,
                  val count: Int) extends Runnable {

  var _size: Int = 1024
  var _md: Map[String, String] = Map()
  var _queue: LinkedBlockingQueue[String] = null
  var written: Int = 0
  var errors: Int = 0
  var done: Boolean = false

  val random = new Random(System.currentTimeMillis() + number * 1111)

  def size(s: Int): WriteWorker = {
    _size = s; this
  }

  def metadata(md: Map[String, String]): WriteWorker = {
    _md = md; this
  }

  def queue(queue: LinkedBlockingQueue[String]): WriteWorker = {
    _queue = queue; this
  }

  override def run() {

    val client = new C3HttpAccessor(host, user, key)

    for (i <- 1 to count) {
      try {
        val ra = client.write(generateDataOfSize(_size), _md)
        if (_queue != null) {
          _queue.offer(ra)
        }
        written = written + 1
      } catch {
        case e: Throwable => {
          errors = errors + 1; System.err.println(e.getMessage)
        }
      }
    }

    done = true
  }

  def generateDataOfSize(size: Int): Array[Byte] = {

    val result = new Array[Byte](size)
    random.nextBytes(result)

    result
  }

}