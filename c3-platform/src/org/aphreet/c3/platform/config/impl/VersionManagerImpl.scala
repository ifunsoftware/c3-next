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
package org.aphreet.c3.platform.config.impl

import org.springframework.osgi.context.BundleContextAware
import collection.mutable.Map
import collection.mutable.HashMap
import org.osgi.framework.BundleContext
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.config.VersionManager
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.logging.LogFactory

@Component("versionManager")
class VersionManagerImpl extends VersionManager with BundleContextAware{

  val log = LogFactory.getLog(getClass)

  var bundleContext:BundleContext = null;

  @Autowired
  override def setBundleContext(bundleContext_ :BundleContext){
    bundleContext = bundleContext_
  }

  override def listC3Modules:Map[String, String] = {

    val map = new HashMap[String, String]

    bundleContext.getBundles
            .filter(b => b.getSymbolicName.startsWith("org.aphreet.c3"))
            .foreach(b => map + (b.getSymbolicName -> b.getHeaders.get("Bundle-Version").toString))

    log.info("Found modules: " + map.toString)

    map
  }
}