/*
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
import org.easymock.EasyMock._
import junit.framework.Assert._
import org.aphreet.c3.platform.accesscontrol.{AccessControlException, UPDATE, READ}
import org.aphreet.c3.platform.domain._
import impl.{RestDomainAccessTokenFactory, DomainAccessToken, DomainAccessTokenFactory}


class DomainAccessTokenFactoryTestCase extends TestCase{

  def testAnonymousDomain(){

    val expectedDomain = Domain("anonymous-id", "anonymous", "", FullMode)

    val domainManager:DomainManager = createMock(classOf[DomainManager])
    expect(domainManager.getDefaultDomainId).andReturn("anonymous-id")
    expect(domainManager.checkDomainAccess("anonymous-id", "", "anonymous-id")).andReturn(expectedDomain)
    replay(domainManager)

    val tokenFactory = new RestDomainAccessTokenFactory
    tokenFactory.domainManager = domainManager

    val token = tokenFactory.createAccessToken(READ, Map("some key" -> "some value"))

    assertEquals(Domain.ACCESS_TOKEN_NAME, token.name)
    assertEquals(READ, token.action)
    assertEquals(expectedDomain, token.asInstanceOf[DomainAccessToken].domain)
  }

  def testAnonymousDomainRO(){

    val expectedDomain = Domain("anonymous-id", "anonymous", "", ReadOnlyMode)

    val domainManager:DomainManager = createMock(classOf[DomainManager])
    expect(domainManager.getDefaultDomainId).andReturn("anonymous-id").times(2)
    expect(domainManager.checkDomainAccess("anonymous-id", "", "anonymous-id")).andReturn(expectedDomain).times(2)
    replay(domainManager)

    val tokenFactory = new RestDomainAccessTokenFactory
    tokenFactory.domainManager = domainManager

    val token = tokenFactory.createAccessToken(READ, Map("" -> ""))

    assertEquals(Domain.ACCESS_TOKEN_NAME, token.name)
    assertEquals(READ, token.action)
    assertEquals(expectedDomain, token.asInstanceOf[DomainAccessToken].domain)

    try{
      tokenFactory.createAccessToken(UPDATE, Map("" -> ""))

      assertTrue(false)
    }catch{
      case e:AccessControlException =>
      case e: Throwable => e.printStackTrace(); assertTrue(false)
    }

    verify(domainManager)
  }

  def testAnonymousDomainDisabled(){
    val expectedDomain = Domain("anonymous-id", "anonymous", "", DisabledMode)

    val domainManager:DomainManager = createMock(classOf[DomainManager])
    expect(domainManager.getDefaultDomainId).andReturn("anonymous-id").times(2)
    expect(domainManager.checkDomainAccess("anonymous-id", "", "anonymous-id")).andReturn(expectedDomain).times(2)
    replay(domainManager)

    val tokenFactory = new RestDomainAccessTokenFactory
    tokenFactory.domainManager = domainManager

    try{
      tokenFactory.createAccessToken(READ, Map("" -> ""))

      assertTrue(false)
    }catch{
      case e:AccessControlException =>
      case e: Throwable => assertTrue(false)
    }

    try{
      tokenFactory.createAccessToken(UPDATE, Map("" -> ""))

      assertTrue(false)
    }catch{
      case e:AccessControlException =>
      case e: Throwable => assertTrue(false)
    }

    verify(domainManager)
  }

  def testNamedDomain(){
    val domainManager:DomainManager = createMock(classOf[DomainManager])

    val keyBase = "/rest/fs/directory/file.txtSun, 13 Mar 2011 23:37:51 MSKplab"
    val hash = "ee358e0b804ae5b18bc3ecf3924bee7dbc1c01425ab48e3dcb1990a038d2ea70"

    expect(domainManager.checkDomainAccess("plab", hash, keyBase)).andReturn(Domain("domain-id", "plab", "", FullMode))

    replay(domainManager)

    val tokenFactory = new RestDomainAccessTokenFactory
    tokenFactory.domainManager = domainManager

    val token = tokenFactory.createAccessToken(READ,
      Map("x-c3-domain" -> "plab",
        "x-c3-sign" -> hash,
        "x-c3-date" -> "Sun, 13 Mar 2011 23:37:51 MSK",
        "x-c3-request-uri" -> "/rest/fs/directory/file.txt"))

    assertEquals(Domain.ACCESS_TOKEN_NAME, token.name)
    assertEquals(READ, token.action)
    assertEquals(Domain("domain-id", "plab", "", FullMode), token.asInstanceOf[DomainAccessToken].domain)

    verify(domainManager)
  }

  def testNamedDomainNoSign(){
    val domainManager:DomainManager = createMock(classOf[DomainManager])

    val keyBase = "/rest/fs/directory/file.txtSun, 13 Mar 2011 23:37:51 MSKplab"

    expect(domainManager.checkDomainAccess("plab", "", keyBase)).andThrow(new DomainException())

    replay(domainManager)

    val tokenFactory = new RestDomainAccessTokenFactory
    tokenFactory.domainManager = domainManager

    try{
      tokenFactory.createAccessToken(READ,
        Map("x-c3-domain" -> "plab",
          "x-c3-date" -> "Sun, 13 Mar 2011 23:37:51 MSK",
          "x-c3-request-uri" -> "/rest/fs/directory/file.txt"))

      assertTrue(false)
    }catch{
      case e:DomainException =>
      case e: Throwable => e.printStackTrace()
      assertTrue(false)
    }

    verify(domainManager)
  }

  def testNamedDomainNoDate(){
    val domainManager:DomainManager = createMock(classOf[DomainManager])

    val keyBase = "/rest/fs/directory/file.txtplab"

    expect(domainManager.checkDomainAccess("plab", "", keyBase)).andThrow(new DomainException())

    replay(domainManager)

    val tokenFactory = new RestDomainAccessTokenFactory
    tokenFactory.domainManager = domainManager

    try{
      tokenFactory.createAccessToken(READ,
        Map("x-c3-domain" -> "plab",
          "x-c3-request-uri" -> "/rest/fs/directory/file.txt"))

      assertTrue(false)
    }catch{
      case e:DomainException =>
      case e: Throwable => assertTrue(false)
    }

    verify(domainManager)
  }
}