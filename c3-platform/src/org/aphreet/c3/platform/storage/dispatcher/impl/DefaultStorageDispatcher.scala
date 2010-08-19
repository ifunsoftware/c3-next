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
package org.aphreet.c3.platform.storage.dispatcher.impl

import org.aphreet.c3.platform.resource.Resource

import scala.collection.mutable.HashMap

import org.aphreet.c3.platform.storage.dispatcher.selector.mime._
import org.aphreet.c3.platform.storage.dispatcher.selector.size._

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.storage.dispatcher.StorageDispatcher
import org.aphreet.c3.platform.storage.Storage

@Component
class DefaultStorageDispatcher extends StorageDispatcher {

  val random = new java.util.Random(System.currentTimeMillis)

  var storages = new HashMap[String, List[Storage]]
  
  val default = "PureBDBStorage"
  
  var mimeSelector:MimeTypeStorageSelector = null
  
  var sizeSelector:SizeStorageSelector = null
  
  @Autowired
  def setMimeTypeStorageSelector(selector:MimeTypeStorageSelector) = {mimeSelector = selector}
  
  @Autowired
  def setSizeStorageSelector(selector:SizeStorageSelector) = {sizeSelector = selector}
  
  def setStorages(sts:List[Storage]) = {
    val newStorages = new HashMap[String, List[Storage]]
    
    for(s <- sts){
      newStorages.get(s.name) match {
        case Some(xs) => newStorages.put(s.name, xs ::: List(s))
        case None => newStorages.put(s.name, List(s))
      } 
    }
    
    storages = newStorages
  }
  
  def selectStorageForResource(resource:Resource):Storage = {
    
    var storageType = mimeSelector.storageTypeForResource(resource)
    
    if(storageType == null){
    	storageType = sizeSelector.storageTypeForResource(resource)
    }
    
    if(storageType == null){
      storageType = (default, false)
    }
    
    
    resource.isVersioned = storageType._2
    
    selectStorageForName(storageType._1)
  }
  
  private def selectStorageForName(name:String):Storage =
    storages.get(name) match {
      case Some(sx) => random(sx)
      case None => storages.get(default) match{
        case Some(st) => random(st)
        case None => null
      }
    }
  
  
  def random(list:List[Storage]):Storage = {
    val onlineList = list.filter(s => s.mode.allowWrite)
    
    if(onlineList.isEmpty){
      null
    }else{
      val num = math.abs(random.nextInt) % (onlineList.size)
      onlineList.drop(num).head
    }
  }
}
