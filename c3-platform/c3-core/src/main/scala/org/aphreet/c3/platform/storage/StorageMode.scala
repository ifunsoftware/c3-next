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

import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.exception.StorageException

abstract sealed class StorageMode(val name:String, val message:String){
  
  def allowWrite:Boolean
  
  def allowRead:Boolean

  override def toString = name + "(" + message + ")"
  
}

case class RW(msg:String) extends StorageMode("RW", msg){
  
  def this() = this(STORAGE_MODE_NONE)
  
  def allowWrite = true
  
  def allowRead = true
  
}

case class RO(msg:String) extends StorageMode("RO", msg){
  
  def this() = this(STORAGE_MODE_NONE)
  
  def allowWrite = false
  
  def allowRead = true
  
}

case class U(msg:String) extends StorageMode("U", msg){
  
  def this() = this(STORAGE_MODE_NONE)
  
  def allowWrite = false
  
  def allowRead = false
}


object StorageModeParser{

  def valueOf(name:String, message:String):StorageMode = {
    name match {
      case "RW" => RW(message)
      case "RO" => RO(message)
      case "U" => U(message)
      case _ => throw new StorageException("No such mode " + name)
    }
  }

  def valueOf(mode:String):StorageMode = {

    val parts = mode.split("\\(", 2)
    if(parts.size == 2){
      val modeName = parts(0)
      val modeValue = parts(1).replaceFirst("\\)$", "")
      valueOf(modeName, modeValue)
    }else{
      throw new StorageException("Can't parse " + mode)
    }
  }
}