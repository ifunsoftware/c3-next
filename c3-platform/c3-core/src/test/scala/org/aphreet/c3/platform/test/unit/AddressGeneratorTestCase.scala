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

package org.aphreet.c3.platform.test.unit

import org.aphreet.c3.platform.resource.AddressGenerator
import junit.framework.{Assert, TestCase}
import java.util.Random

class AddressGeneratorTestCase extends TestCase {

  def testAddressGeneration() {

    val systemId:Int = 0xAFAFBDFD
    val storageId = "1234"

    val address = AddressGenerator.addressForStorage(storageId, systemId)

    val expectedEnd = systemId.toHexString.toLowerCase + "-" + storageId

    Assert.assertTrue("Address does not end with systemid-storageid", address.endsWith(expectedEnd))
  }

  def testByteOps = {
    println("Address: " + generateId(6))
  }

  def testGenerateSystemId = {
    println("SystemId: " + generateId(2))
  }

  def testGenerateStorageId = {
   println("StorageId: " + generateId(1))
  }

  def generateId(groups:Int):String = {
    val chars = Array("0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                      "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                      "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                      "u", "v", "w", "x", "y", "z", "A", "B", "C", "D",
                      "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
                      "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
                      "Y", "Z", "Z", "Z")

    val bytes = Array.ofDim[Byte](groups * 3);

    val random = new Random

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

      if(i != 0 && i!=1 && i != 5){
        builder.append("-")
      }

      builder.append(chars(res0)).append(chars(res1)).append(chars(res2)).append(chars(res3))
    }

    builder.toString()
  }

}