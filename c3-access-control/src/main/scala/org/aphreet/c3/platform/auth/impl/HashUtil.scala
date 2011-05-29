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

package org.aphreet.c3.platform.auth.impl

import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

object HashUtil{


  def hash(input:String, algorithm:DigestAlgorithm):String = {
    if (input == null || input.isEmpty) return ""

    val hexString = new StringBuilder

    val md = MessageDigest.getInstance(algorithm.name)
    md.update(input.getBytes())

    val hash = md.digest

    for (b <- hash) {
      if ((0xFF & b) < 0x10) {
        hexString.append("0").append(Integer.toHexString((0xFF & b)))
      } else {
        hexString.append(Integer.toHexString((0xFF & b)))
      }
    }

    hexString.toString
  }

  def hmac(key:String, input:String):String = {

    val mac = Mac.getInstance("HmacSHA256")

    val secret = new SecretKeySpec(key.getBytes, "HmacSHA256")
    mac.init(secret)

    val digest = mac.doFinal(input.getBytes("UTF-8"));

    val hexString = new StringBuilder

    for (b <- digest) {
      if ((0xFF & b) < 0x10) {
        hexString.append("0").append(Integer.toHexString((0xFF & b)))
      } else {
        hexString.append(Integer.toHexString((0xFF & b)))
      }
    }

    hexString.toString
  }

  def md5hash(input: String): String = hash(input, MD5)

  def sha256hash(input:String):String = hash(input, SHA256)
}

sealed class DigestAlgorithm(val name:String)

object MD5 extends DigestAlgorithm("MD5")
object SHA256 extends DigestAlgorithm("SHA-256")