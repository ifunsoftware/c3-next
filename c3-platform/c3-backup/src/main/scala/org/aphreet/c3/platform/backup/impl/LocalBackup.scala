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
import org.aphreet.c3.platform.backup.{LocalBackupLocation, BackupLocation, AbstractBackup}
import java.io.{File, FileReader, BufferedReader, FileInputStream}
import org.apache.commons.codec.digest.DigestUtils
import org.aphreet.c3.platform.common.Disposable._
import org.aphreet.c3.platform.exception.PlatformException
import scala.collection.mutable.ListBuffer


class LocalBackup(val path: File, val create: Boolean) extends AbstractBackup {

  {
    if (!create) {
      if (!LocalBackup.hasValidChecksum(path)) {
        throw new PlatformException("Invalid checksum for backup " + path)
      }
    }

    path.getParentFile.mkdirs()

    val env = new util.HashMap[String, String]()
    env.put("create", create.toString)
    zipFs = FileSystems.newFileSystem(URI.create("jar:file:" + path.getAbsolutePath), env, null)

    zipFilePath = path.getAbsolutePath
    md5FilePath = zipFilePath + ".md5"
  }

}

object LocalBackup {
  def listBackups(target: LocalBackupLocation): List[String] = {
    val listBuffer = new ListBuffer[String]()
    val folder = new File(target.directory)

    if (folder.exists() && folder.isDirectory) {
      val filesList = folder.listFiles()

      filesList
        .filter(file => file.isFile && file.getName.endsWith(".zip") && hasValidChecksum(file))
        .foreach(file => listBuffer += file.getAbsolutePath)
    }

    listBuffer.toList
  }

  def hasValidChecksum(path: File): Boolean = {
    var isValid = false

    using(new FileInputStream(path))(is => {
      val calculatedMd5 = DigestUtils.md5Hex(is)

      val md5File = new File(path + ".md5")

      if (md5File.exists() && md5File.isFile) {
        using(new BufferedReader(new FileReader(md5File)))(reader => {
          isValid = reader.readLine().equals(calculatedMd5)
        })
      }
    })

    isValid
  }
}

