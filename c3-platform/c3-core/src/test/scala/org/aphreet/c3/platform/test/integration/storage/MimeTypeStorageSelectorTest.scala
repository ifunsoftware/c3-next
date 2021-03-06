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
package org.aphreet.c3.platform.test.integration.storage

import junit.framework.Assert._
import junit.framework.TestCase
import org.aphreet.c3.platform.config.impl.MemoryConfigPersister
import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigPersister, PlatformConfigComponent}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.storage.dispatcher.selector.mime._


class MimeTypeStorageSelectorTest extends TestCase {

  def testConfigPersistence() {

    val testConfigPersister = new MemoryConfigPersister

    val configAccessor = new MimeTypeConfigAccessor(testConfigPersister)

    val config = Map(
      "*/*" -> false,
      "image/*" -> true,
      "image/png" -> true
    )

    configAccessor.store(config)

    assertEquals(config, configAccessor.load)

    val app = new Object with PlatformConfigComponent with MimeTypeStorageSelectorComponent {
      def platformConfigManager: PlatformConfigManager = null

      def configPersister: ConfigPersister = testConfigPersister
    }

    val selector = app.mimeStorageSelector

    assertEquals(true,selector.storageTypeForResource(resourceWithType("image/png")))
    assertEquals(true,selector.storageTypeForResource(resourceWithType("image/jpeg")))
    assertEquals(false,selector.storageTypeForResource(resourceWithType("application/pdf")))

  }

  def resourceWithType(mimeType: String): Resource = {
    new Resource().withMetadata(Map(Resource.MD_CONTENT_TYPE -> mimeType))
  }
}
