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
package org.aphreet.c3.platform.common

import java.io.File
import java.lang.Character

/*
 * Abstract path representation
 * Represent path with delimiters "/"
 */

class Path (path:String){

  private val correctPath:String = getCorrectPath(path)
  
  def this(file:File) = this(file.getAbsolutePath)

  def file:File = new File(correctPath)

  def stringValue:String = correctPath

  def append(path:String):Path = Path(this.stringValue + "/" + path)
  
  private def getCorrectPath(path:String):String = {
    
    var newPath:String = path.trim;
    
    if(newPath matches """[A-Za-z]:[\\/].*"""){
      val driveLetter = Character.toUpperCase(path.charAt(0))
      
      newPath = driveLetter + path.substring(1)
    }

    newPath.replaceAll("""\\+""", """/""").replaceFirst("/$","")
  }
  
  override def toString:String = correctPath

  override def equals(that:Any):Boolean = {
    if(that == null) return false
    
    if(!that.isInstanceOf[Path]) return false
    
    val thatPath = that.asInstanceOf[Path]
    
    thatPath.correctPath == this.correctPath
  }
  
  override def hashCode:Int = 
    this.correctPath.hashCode

}

object Path{

  def apply(path:String):Path = new Path(path)

}