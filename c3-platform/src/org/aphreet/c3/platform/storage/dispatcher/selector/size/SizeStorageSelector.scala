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
package org.aphreet.c3.platform.storage.dispatcher.selector.size

import org.aphreet.c3.platform.resource.Resource

import scala.collection.immutable.{SortedMap, TreeMap}

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.storage.dispatcher.selector.AbstractStorageSelector

@Component
class SizeStorageSelector extends AbstractStorageSelector[Long]{

  private var sizeRanges:SortedMap[Long, (String, Boolean)] = null
  
  @Autowired
  def setSizeSelectorConfigAccessor(accessor:SizeSelectorConfigAccessor) = {configAccessor = accessor}
  
  override def storageTypeForResource(resource:Resource):(String,Boolean) = {
    val size = resource.versions(0).data.length
    storageTypeForSize(size)
  }
  
  def storageTypeForSize(size:Long):(String,Boolean) = {
    for(sizeRange <- sizeRanges){
      if(size >= sizeRange._1)
        return sizeRange._2
    }
    null
  }
  
  override def updateConfig(config:Map[Long, (String,Boolean)]) = {
    sizeRanges = new TreeMap[Long, (String,Boolean)]()(new ReverseOrdering) ++ config
  }
  
  override def configEntries:List[(Long, String, Boolean)] = {
    sizeRanges.map(entry => (entry._1, entry._2._1, entry._2._2)).toList
  }
  
}

class ReverseOrdering extends Ordering[Long] {
  
  override def compare(x:Long, y:Long):Int = {
    if(x > y) return 1
    if(x < y) return -1
    0
  }
}
