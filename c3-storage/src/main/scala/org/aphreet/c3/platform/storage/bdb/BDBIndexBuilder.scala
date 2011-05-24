package org.aphreet.c3.platform.storage.bdb

import org.aphreet.c3.platform.storage.StorageIndex
import org.aphreet.c3.platform.resource.Resource
import collection.Map

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

class BDBIndexBuilder(val index: StorageIndex) {
  
  def createKey(params: Map[String, String]): Array[Byte] = {

    if (index.fields.size == 1) {

      params.get(index.fields.head) match {
        case Some(x) => createKey(x)
        case None => null
      }

    } else {

      val builder = new StringBuilder

      for (field <- index.fields) {
        params.get(field) match {
          case Some(value) => appendToBuilder(value, builder)
          case None => return null
        }
      }

      builder.toString.getBytes("UTF-8")
    }

  }

  def createKey(resource: Resource): Array[Byte] = {

    val map = if (index.system) {
      resource.systemMetadata
    } else {
      resource.metadata
    }

    createKey(map)

  }

  private def appendToBuilder(value:String, builder:StringBuilder) = {
    builder.append(value).append("%@%")
  }

  def createCompositeKey(list: List[String]): Array[Byte] = {

    val builder = new StringBuilder

    for(e <- list)
      appendToBuilder(e, builder)

    builder.toString.getBytes("UTF-8")
  }

  def createKey(value: String): Array[Byte] = {
    value.getBytes("UTF-8")
  }

}