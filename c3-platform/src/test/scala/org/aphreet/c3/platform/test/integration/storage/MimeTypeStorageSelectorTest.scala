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

import eu.medsea.mimeutil.MimeType

import org.aphreet.c3.platform.storage.dispatcher.selector.mime._

import junit.framework.Assert._
import org.aphreet.c3.platform.test.integration.AbstractTestWithFileSystem
import org.aphreet.c3.platform.config.impl.PlatformConfigManagerImpl
import java.io.File

class MimeTypeStorageSelectorTest extends AbstractTestWithFileSystem{

  def testConfigPersistence = {
    
    val configAccessor = new MimeTypeConfigAccessor
    
    val config = Map(
    	"*/*" -> ("PureBDBStorage", false),
    	"image/*" -> ("FileBDBStorage", true),
    	"image/png" -> ("PureBDBStorage", true)
    )

    val configFile = "c3-mime-types.json"


    configAccessor.storeConfig(config, new File(testDir, configFile))
   
    val configManager = new PlatformConfigManagerImpl
    configManager.configDir = testDir
    
    configAccessor.setConfigManager(configManager)
    
    val selector = new MimeTypeStorageSelector
    selector.setConfigAccessor(configAccessor)
    selector.init
    
    assertEquals(("PureBDBStorage", true),selector.storageTypeForMimeType(new MimeType("image/png")))
    assertEquals(("FileBDBStorage", true),selector.storageTypeForMimeType(new MimeType("image/jpeg")))
    assertEquals(("PureBDBStorage", false),selector.storageTypeForMimeType(new MimeType("application/pdf")))
    
  }
}
