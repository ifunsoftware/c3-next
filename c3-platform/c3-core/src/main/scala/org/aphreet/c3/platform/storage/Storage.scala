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

package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.common.{CloseableIterable, Constants, Path}
import org.aphreet.c3.platform.resource.Resource

import volume.Volume
import org.aphreet.c3.platform.exception.StorageException
import org.apache.commons.logging.LogFactory


/**
 * Base class for all storages
 */
abstract class Storage extends StorageLike with CloseableIterable[Resource]{

  val log = LogFactory.getLog(getClass)

  protected var storageMode:StorageMode = new RW

  /**
   * Volume where this storage have it's data
   */
  var volume:Volume = null

  /**
   * Primary id of this storage
   */
  def id:String

  /**
   * Create new index with specified parameters
   */
  def createIndex(index:StorageIndex)

  /**
   * Remove index from storage
   */
  def removeIndex(index:StorageIndex)

  /**
   * Get current storage parameters
   */
  def params:StorageParams

  /**
   * Get current mode
   */
  def mode:StorageMode = storageMode

  /**
   * Set storage mode
   */
  def mode_=(newMode:StorageMode) {

    log info "Setting storage mode " + newMode

    if(newMode.message == Constants.STORAGE_MODE_MAINTAIN){
      storageMode = newMode
    }else{
      mode.message match {
        case Constants.STORAGE_MODE_MAINTAIN => {
          storageMode = newMode
        }

        case Constants.STORAGE_MODE_MIGRATION => {
          mode match {
            case RW(msg) => {
              if(newMode.allowWrite){
                storageMode = newMode
              }
            }
            case RO(msg) => {
              if((!newMode.allowRead || newMode.allowWrite)
                      && newMode.message != Constants.STORAGE_MODE_USER){
                storageMode = newMode
              }
            }

            case U(msg) => {}
          }
        }
        case Constants.STORAGE_MODE_USER => {

          mode match {
            case RO(msg) => {
              if(newMode.message == Constants.STORAGE_MODE_USER){
                storageMode = newMode
              }
            }

            case U(msg) => {
              if(newMode.message == Constants.STORAGE_MODE_USER){
                storageMode = newMode
              }
            }

            case RW(msg) => {
              storageMode = newMode
            }
          }

        }
        case Constants.STORAGE_MODE_CAPACITY => {
          newMode.message match{
            case Constants.STORAGE_MODE_CAPACITY => storageMode = newMode
            case Constants.STORAGE_MODE_MIGRATION => {
              if(!newMode.allowWrite){
                storageMode = newMode
              }
            }
          }


        }
        case Constants.STORAGE_MODE_NONE => {
          if(newMode.message == Constants.STORAGE_MODE_NONE){
            storageMode = newMode
          }else{
            mode match {
              case RW(msg) => storageMode = newMode
              case _ => {}
            }
          }

        }
        case _ => {}
      }
    }

    if(this.mode != newMode) {
      throw new StorageException("Failed to set mode " + newMode + " due to current mode: " + this.mode)
    }

    if(this.mode.allowWrite && this.mode.message == Constants.STORAGE_MODE_USER){
      storageMode = RW(Constants.STORAGE_MODE_NONE)
    }

  }

  /**
   * Resource count in this storage
   */
  def count:Long

  /**
   * Size that is used by storage on disk
   */
  def usedCapacity:Long

  /**
   * Create new storage iterator
   */
  def iterator(fields:Map[String,String] = Map(),
               systemFields:Map[String,String] = Map(),
               filter:(Resource) => Boolean = ((resource:Resource) => true)
          ):StorageIterator

  /**
   * Creates new storage iterator
   */

  def iterator:StorageIterator

  /**
   * Close storage
   */
  def close()


  /**
   * Path where storage is located
   */
  def path:Path

  /**
   * Full path to storage including #{path} and storage name with ID
   */
  def fullPath:Path

  /**
   * Name of this storage
   */
  def name:String
}
