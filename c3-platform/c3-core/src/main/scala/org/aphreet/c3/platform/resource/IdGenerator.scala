/**
 * Copyright (c) 2011, Mikhail Malygin
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

package org.aphreet.c3.platform.resource

import java.util.Random

object IdGenerator{

  val chars = Array("0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                      "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                      "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                      "u", "v", "w", "x", "y", "z", "A", "B", "C", "D",
                      "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
                      "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
                      "Y", "Z", "~", ".")


  def generateAddress(seedDiff:Long, systemId:String, storageId:String) = {
   generateId(seedDiff, 6, true) + "-" + systemId + "-" + storageId
  }

  def generateSystemId:String = {
    generateId(0, 2, true)
  }

  def generateStorageId:String = {
    generateId(0, 1, true)
  }

  def generateId(groups:Int):String = {
    generateId(0, groups, false)
  }

  private def generateId(seedDiff:Long, groups:Int, withDashes:Boolean):String = {


    val bytes = Array.ofDim[Byte](groups * 3)

    val random = new Random(System.currentTimeMillis() + seedDiff * 100 + 5000)

    random.nextBytes(bytes)

    val builder = new StringBuilder


    for(i <- 0 to groups - 1){
      val byte0 = bytes(i * 3 + 0)
      val byte1 = bytes(i * 3 + 1)
      val byte2 = bytes(i * 3 + 2)

      val res0 = (byte0 >> 2) & 0x3F
      val res1 = ((byte1 & 0xF0) >> 4) | ((byte0 & 0x03) << 4)
      val res2 = (byte1 & 0x0F) | ((byte2 & 0xC0) >> 2)
      val res3 = byte2 & 0x3F

      if(withDashes){
        if(i != 0 && i!=1 && i != 5){
          builder.append("-")
        }
      }

      builder.append(chars(res0)).append(chars(res1)).append(chars(res2)).append(chars(res3))
    }

    builder.toString()
  }

  def trailShort(generatedString:String):Short = {

    var shortResult = 0

    shortResult = shortResult | digitValue(generatedString.charAt(0))
    shortResult = (shortResult << 6) | digitValue(generatedString.charAt(1))
    shortResult = (shortResult << 3) | (digitValue(generatedString.charAt(3)) >> 3)

    shortResult.toShort
  }

  private def digitValue(code:Char):Byte = {

    val result = (if(code >= 48 && code <=57){
      code - 48
    }else if (code >= 65 && code <=90){
      code - 65 + 36
    }else if (code >=97 && code <=122){
      code - 97 + 10
    } else if (code == 46) 63 else if (code == 126) 62 else {
      0
    }).toByte

    result
  }
}