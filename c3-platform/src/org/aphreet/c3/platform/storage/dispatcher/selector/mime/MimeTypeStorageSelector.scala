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

import org.aphreet.c3.platform.resource.Resource

import scala.collection.mutable.HashMap

import eu.medsea.mimeutil.MimeType

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import org.aphreet.c3.platform.storage.dispatcher.selector.AbstractStorageSelector

@Component
class MimeTypeStorageSelector extends AbstractStorageSelector[String]{

  private var typeMap = new HashMap[String, (String, Boolean)]
  
  @Autowired
  def setConfigAccessor(accessor:MimeTypeConfigAccessor) = {configAccessor = accessor}
  
  override def storageTypeForResource(resource:Resource):(String,Boolean) = {
    
    val mime = new MimeType(resource.mimeType)

    storageTypeForMimeType(mime) 
  }
  
  def storageTypeForMimeType(mime:MimeType):(String,Boolean) = {
    val mediaType = mime.getMediaType
    val subType = mime.getSubType
    
    typeMap.get(mediaType + "/" + subType) match {
      case Some(entry) => entry
      case None => typeMap.get(mediaType + "/*") match {
        case Some(entry) => entry
        case None => typeMap.get("*/*") match {
          case Some(entry) => entry
          case None => null
        }
      }
    }
  }
  
  override def configEntries:List[(String, String, Boolean)] = 
    typeMap.map(entry => (entry._1, entry._2._1, entry._2._2)).toList
  
  
  override def updateConfig(config:Map[String, (String,Boolean)]) = {
    val map = new HashMap[String, (String,Boolean)]
    for(entry <- config)
      map + entry
    
    typeMap = map
  }
  
}
