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
package org.aphreet.c3.platform.storage.dispatcher.selector.mime

import collection.mutable
import eu.medsea.mimeutil.MimeType
import org.aphreet.c3.platform.config.SystemDirectoryProvider
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.dispatcher.selector.ModifiableStorageSelector


trait MimeTypeStorageSelectorComponent{

  this: SystemDirectoryProvider =>

  val mimeStorageSelector: ModifiableStorageSelector[String] = new MimeTypeStorageSelectorImpl(new MimeTypeConfigAccessor(this))

  class MimeTypeStorageSelectorImpl(val configAccessor: MimeTypeConfigAccessor)
    extends ModifiableStorageSelector[String]{

    private var typeMap = new mutable.HashMap[String, Boolean]

    {
      updateConfig(configAccessor.load)
    }

    override def storageTypeForResource(resource:Resource):Boolean = {
      storageTypeForMimeType(new MimeType(resource.mimeType))
    }

    private def storageTypeForMimeType(mime:MimeType):Boolean = {
      val mediaType = mime.getMediaType
      val subType = mime.getSubType

      typeMap.get(mediaType + "/" + subType) match {
        case Some(entry) => entry
        case None => typeMap.get(mediaType + "/*") match {
          case Some(entry) => entry
          case None => typeMap.get("*/*") match {
            case Some(entry) => entry
            case None => true
          }
        }
      }
    }

    override def configEntries:List[(String, Boolean)] =
      typeMap.map(entry => (entry._1, entry._2)).toList


    def updateConfig(config:Map[String, Boolean]) {
      typeMap = new mutable.HashMap[String, Boolean] ++= config
    }

    def addEntry(entry:(String, Boolean)) {
      configAccessor.update(entries => entries.filter(_._1 != entry._1) +
        ((entry._1, entry._2))
      )
      updateConfig(configAccessor.load)
    }

    def removeEntry(key: String) {
      configAccessor.update(_.filter(_._1 != key))
      updateConfig(configAccessor.load)
    }

  }

}
