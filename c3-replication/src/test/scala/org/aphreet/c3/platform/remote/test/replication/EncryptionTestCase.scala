package org.aphreet.c3.platform.remote.test.replication

import javax.crypto.{SecretKey, Cipher, KeyGenerator}
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import junit.framework.{Assert, TestCase}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, KeyPairGenerator}
import org.aphreet.c3.platform.remote.replication.impl.data.encryption.{AsymmetricDataEncryptor, AsymmetricKeyGenerator, SymmetricKeyGenerator, DataEncryptor}

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: 6/9/11
 * Time: 2:43 PM
 * To change this template use File | Settings | File Templates.
 */

class EncryptionTestCase extends TestCase {

  def testSymmetricEncryption = {

    val text = "this is my text"

    val encodedKey = SymmetricKeyGenerator.generateAESKey

    val encryptor = new DataEncryptor(encodedKey)

    val encrypted = encryptor.encrypt(text.getBytes("UTF-8"))

    val original = encryptor.decrypt(encrypted)

    val originalString = new String(original, "UTF-8")

    Assert.assertEquals(text, originalString)
  }

  def testAsymmetricEncryption = {
    
    val keyPair = AsymmetricKeyGenerator.generateKeys

    val publicKeyRaw = keyPair._1

    val privateKeyRaw = keyPair._2

    val originalText = "this is my text"

    val encrypted = AsymmetricDataEncryptor.encrypt(originalText.getBytes("UTF-8"), publicKeyRaw)

    val decrypted = AsymmetricDataEncryptor.decrypt(encrypted, privateKeyRaw)

    Assert.assertEquals(originalText, new String(decrypted, "UTF-8"))
  }
}