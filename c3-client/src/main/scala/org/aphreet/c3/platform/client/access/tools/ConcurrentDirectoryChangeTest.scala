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

package org.aphreet.c3.platform.client.access.tools

import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.access.http.C3FileHttpAccessor

class ConcurrentDirectoryChangeTest(override val args: Array[String]) extends CLI(args) {

  def clientName = "ConcurrentDirectoryChangeTest"

  def cliDescription = parameters(
    HOST_ARG,
    USER_ARG,
    KEY_ARG,
    THREADS_ARG,
    COUNT_ARG,
    HELP_ARG
  )

  def run() {

    val targetHost: String = HOST_ARG

    if (targetHost.isEmpty){
      helpAndExit(clientName)
    }else{
      writeDirectories(HOST_ARG, USER_ARG, KEY_ARG, THREADS_ARG, COUNT_ARG)
    }

  }

  def writeDirectories(host: String, domain: String, key: String, threads: String, count: String){
    (1 to threads.toInt)
      .map(thread => submitWriter(host, domain, key, count.toInt, "child-" + String.format("%03d", Integer.valueOf(thread))))
      .foreach(_.join())
  }

  def submitWriter(host: String,
                   domain: String,
                   key: String,
                   count: Int,
                   directoryBaseName: String): Thread = {
    val thread = new Thread(new Runnable {
      def run() {

        val fsAccessor = new C3FileHttpAccessor(host, domain, key)

        for (dirNumber <- 1 to count){
          val dirName = "/" + String.format("%03d", Integer.valueOf(dirNumber)) + "-" + directoryBaseName
          println(dirName)
          fsAccessor.makeDir(dirName)
        }
      }
    })

    thread.start()

    thread
  }
}
