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

package org.aphreet.c3.platform.remote.test.domain

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.domain.impl.DomainManagerImpl
import org.aphreet.c3.platform.domain.{ReadOnlyMode, FullMode, Domain}

class DomainManagerTestCase extends TestCase{

  def testDomainAdd() {

    val domain1 = Domain("1", "name", "key", FullMode, deleted = false)
    val domain2 = Domain("2", "name2", "key2", FullMode, deleted = false)


    val domainManager = new DomainManagerImpl

    val existentDomains = List(domain1,
                               domain2)

    val newDomain = Domain("3", "name3", "key3", FullMode, deleted = false)

    val newDomainList = domainManager.addDomainToList(newDomain, "systemId", existentDomains)

    assertTrue(newDomainList.contains(domain1))
    assertTrue(newDomainList.contains(domain2))
    assertTrue(newDomainList.contains(newDomain))
    assertTrue(newDomain.name == "name3")
  }

  def testDomainAddWithRename() {

    val domain1 = Domain("1", "name", "key", FullMode, deleted = false)
    val domain2 = Domain("2", "name2", "key2", FullMode, deleted = false)


    val domainManager = new DomainManagerImpl

    val existentDomains = List(domain1,
                               domain2)

    val newDomain = Domain("3", "name", "key3", FullMode, deleted = false)

    val newDomainList = domainManager.addDomainToList(newDomain, "systemId", existentDomains)

    assertTrue(newDomainList.contains(domain1))
    assertTrue(newDomainList.contains(domain2))
    assertTrue(newDomainList.contains(newDomain))
    assertTrue(newDomain.name == "name-systemId")
  }

  def testDomainUpdate() {

    val domain1 = Domain("1", "name", "key", FullMode, deleted = false)
    val domain2 = Domain("2", "name2", "key2", FullMode, deleted = false)


    val domainManager = new DomainManagerImpl

    val existentDomains = List(domain1,
                               domain2)

    val newDomain = Domain("1", "name3", "key3", ReadOnlyMode, deleted = false)

    val newDomainList = domainManager.addDomainToList(newDomain, "systemId", existentDomains)

    assertTrue(newDomainList.contains(domain1))
    assertTrue(domain1.id == "1")
    assertTrue(domain1.name == "name3")
    assertTrue(domain1.key == "key3")
    assertTrue(domain1.mode == ReadOnlyMode)

    assertTrue(newDomainList.contains(domain2))

    assertTrue(newDomainList.size == 2)

  }

  def testDomainUpdateWithoutRename() {

    val domain1 = Domain("1", "name-systemId", "key", FullMode, deleted = false)
    val domain2 = Domain("2", "name2", "key2", FullMode, deleted = false)


    val domainManager = new DomainManagerImpl

    val existentDomains = List(domain1,
                               domain2)

    val newDomain = Domain("1", "name", "key3", ReadOnlyMode, deleted = false)

    val newDomainList = domainManager.addDomainToList(newDomain, "systemId", existentDomains)

    assertTrue(newDomainList.contains(domain1))
    assertTrue(domain1.id == "1")
    assertTrue(domain1.name == "name")
    assertTrue(domain1.key == "key3")
    assertTrue(domain1.mode == ReadOnlyMode)

    assertTrue(newDomainList.contains(domain2))

    assertTrue(newDomainList.size == 2)

  }

//  def testDomainUpdateWithRename() {
//
//    val domain1 = Domain("1", "name", "key", FullMode)
//    val domain2 = Domain("2", "name2", "key2", FullMode)
//
//
//    val domainManager = new DomainManagerImpl
//
//    val existentDomains = List(domain1,
//                               domain2)
//
//    val newDomain = Domain("1", "name2", "key3", ReadOnlyMode)
//
//    val newDomainList = domainManager.addDomainToList(newDomain, "systemId", existentDomains)
//
//    assertTrue(newDomainList.contains(domain1))
//    assertTrue(domain1.id == "1")
//    assertTrue(domain1.name == "name2-systemId")
//    assertTrue(domain1.key == "key3")
//    assertTrue(domain1.mode == ReadOnlyMode)
//
//    assertTrue(newDomainList.contains(domain2))
//
//    assertTrue(newDomainList.size == 2)
//
//  }
}