/*
 * Copyright (c) 2012, Mikhail Malygin
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

package org.aphreet.c3.platform.backup.impl

import org.aphreet.c3.platform.common.{Path => C3Path}
import java.nio.file._
import java.util
import java.net.URI
import org.aphreet.c3.platform.backup.AbstractBackup
import java.io.{File, FileReader, BufferedReader, FileInputStream}
import org.apache.commons.codec.digest.DigestUtils
import org.aphreet.c3.platform.common.Disposable._


class Backup(val uri:URI, val create:Boolean) extends AbstractBackup {

  {
    val env = new util.HashMap[String, String]()
    env.put("create", create.toString)
    zipFs = FileSystems.newFileSystem(uri, env, null)

    zipFilePath = uri.toString.substring("jar:file:".length)
    md5FilePath = zipFilePath + ".md5"
  }
}


object Backup{

  def open(path:C3Path):Backup = {
    val zipFile = URI.create("jar:file:" + path)

    if (hasValidChecksum(path.stringValue)) {
      new Backup(zipFile, false)
    } else {
      throw new IllegalStateException("Backup " + path + " doesn't have valid checksum")
    }
  }

  def create(path:C3Path):Backup = {
    val zipFile = URI.create("jar:file:" + path)
    new Backup(zipFile, true)
  }

  def directoryForAddress(fs:FileSystem, address:String):Path = {
    val firstLetter = address.charAt(0).toString
    val secondLetter = address.charAt(1).toString
    val thirdLetter = address.charAt(2).toString

    fs.getPath(firstLetter, secondLetter, thirdLetter)
  }

  def hasValidChecksum(path : String): Boolean = {
    var isValid = false

    using(new FileInputStream(path)) (is => {
      val calculatedMd5 = DigestUtils.md5Hex(is)

      val md5File = new File(path + ".md5")

      if (md5File.exists() && md5File.isFile) {
        using(new BufferedReader(new FileReader(md5File))) (reader => {
          isValid = reader.readLine().equals(calculatedMd5)
        })
      }
    })

    isValid
  }
}
