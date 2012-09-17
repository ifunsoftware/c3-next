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

package org.aphreet.c3.platform.remote.replication.impl.config

import org.aphreet.c3.platform.remote.api.management.StorageDescription
import org.aphreet.c3.platform.storage.{StorageModeParser, Storage}
import org.aphreet.c3.platform.exception.StorageException
import collection.mutable.{ArrayBuffer, HashSet, HashMap}


/*        .==.        .==.          
         //`^\\      //^`\\
        // ^ ^\(\__/)/^ ^^\\
       //^ ^^ ^/6  6\ ^^ ^ \\
      //^ ^^ ^/( .. )\^ ^ ^ \\
     // ^^ ^/\| v""v |/\^ ^ ^\\
    // ^^/\/ /  `~~`  \ \/\^ ^\\
    -----------------------------
*/

class StorageSynchronizer extends DtoConvertor{

  /**
   * Compare storages from this and other machines and
   * decide what ids should be added
   * to remote machine
   */
  def getAdditionalIds(remoteStorages:List[StorageDescription], localStorages:List[Storage]):List[(String, String)] = {
    val localStorageDescriptions = localStorages.map(s => storageToDescription(s)).toList

    compareStorageConfigs(localStorageDescriptions, remoteStorages)

  }

  def compareStorageConfigs(referenceConfiguration:List[StorageDescription], updatableConfiguration:List[StorageDescription])
  :List[(String, String)] = {

    val remoteIdsSet = new HashSet[String]

    val remoteTypesMap = new HashMap[String, ArrayBuffer[String]]

    for(remoteStorage <- updatableConfiguration){

      val mode = StorageModeParser.valueOf(remoteStorage.mode)

      remoteIdsSet += remoteStorage.id

      if(remoteStorage.ids != null){
        remoteIdsSet ++= remoteStorage.ids.toList
      }

      if(mode.allowWrite){

        remoteTypesMap.get(remoteStorage.storageType) match{
          case Some(buffer) => buffer += remoteStorage.id
          case None => {
            remoteTypesMap.put(remoteStorage.storageType, ArrayBuffer(remoteStorage.id))
          }
        }
      }

    }

    val localTypesMap = new HashMap[String, ArrayBuffer[String]]

    for(localStorage <- referenceConfiguration){

      val idBuffer = localTypesMap.get(localStorage.storageType) match{
        case Some(buffer) => buffer
        case None => {
          val buffer = new ArrayBuffer[String]()
          localTypesMap += ((localStorage.storageType, buffer))
          buffer
        }
      }

      idBuffer += localStorage.id

      if(localStorage.ids != null){
        idBuffer ++= localStorage.ids
      }

    }


    var result = List[(String, String)]()

    for((localType, idsBuffer) <- localTypesMap){

      val remoteBuffer = remoteTypesMap.get(localType) match{
        case Some(buffer) => buffer
        case None => throw new StorageSynchronizerException("Can't find remote storage to store data from local " + localType)
      }

      if(remoteBuffer.isEmpty){
        throw new StorageSynchronizerException("Can't find remote storage to store data from local " + localType)
      }

      var iterator = remoteBuffer.iterator

      for(localId <- idsBuffer){
        if(!remoteIdsSet.contains(localId)){

          val currentRemotePrimId = if(iterator.hasNext){
            iterator.next()
          }else{
            iterator = remoteBuffer.iterator
            iterator.next()
          }
          result = (currentRemotePrimId, localId) :: result
        }
      }

    }

    result

  }

  def getAdditionalId(storages:List[Storage], secondaryId:String, storageType:String):Option[String] = {

    val idNotExists = storages.filter(s => s.ids.contains(secondaryId) || s.id == secondaryId).isEmpty

    if(idNotExists){

      storages.filter(s => (s.name == storageType
              && s.mode.allowWrite))
              .sortWith((s1, s2) => s1.ids.size < s2.ids.size).toList.headOption match {

        case Some(s) => Some(s.id)
        case None => throw new StorageSynchronizerException("Can't find remote storage to store data from local " + storageType)
      }
    }else{
      None
    }

  }
}

class StorageSynchronizerException(override val message:String, override val cause:Throwable) extends StorageException(message, cause){

  def this(message:String) = this(message, null)

  def this() = this(null, null)

  def this(cause:Throwable) = this(null, cause)

}