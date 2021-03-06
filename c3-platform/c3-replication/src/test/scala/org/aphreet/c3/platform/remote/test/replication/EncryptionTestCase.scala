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
package org.aphreet.c3.platform.remote.test.replication

import junit.framework.{Assert, TestCase}
import org.aphreet.c3.platform.remote.replication.impl.data.encryption.{AsymmetricDataEncryptor, AsymmetricKeyGenerator, SymmetricKeyGenerator, DataEncryptor}


class EncryptionTestCase extends TestCase {

  def testSymmetricEncryption(){

    val text = "this is my text"

    val encodedKey = SymmetricKeyGenerator.generateAESKey

    val encryptor = new DataEncryptor(encodedKey)

    val encrypted = encryptor.encrypt(text.getBytes("UTF-8"))

    val original = encryptor.decrypt(encrypted)

    val originalString = new String(original, "UTF-8")

    Assert.assertEquals(text, originalString)
  }

  def testAsymmetricEncryption(){
    
    val keyPair = AsymmetricKeyGenerator.generateKeys

    val publicKeyRaw = keyPair._1

    val privateKeyRaw = keyPair._2

    val originalText = "this is my text"

    val encrypted = AsymmetricDataEncryptor.encrypt(originalText.getBytes("UTF-8"), publicKeyRaw)

    val decrypted = AsymmetricDataEncryptor.decrypt(encrypted, privateKeyRaw)

    Assert.assertEquals(originalText, new String(decrypted, "UTF-8"))
  }
}
