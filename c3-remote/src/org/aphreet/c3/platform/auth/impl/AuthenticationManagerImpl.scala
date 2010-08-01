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


import org.aphreet.c3.platform.auth._
import org.aphreet.c3.platform.auth.exception._

import javax.annotation.PostConstruct
import java.security.MessageDigest

import collection.mutable.HashMap

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component("authenticationManager")
class AuthenticationManagerImpl extends AuthenticationManager {
  val users = new HashMap[String, User]

  var configAccessor: AuthConfigAccessor = _

  @Autowired
  def setConfigAccessor(accessor: AuthConfigAccessor) = {configAccessor = accessor}

  @PostConstruct
  def init {
    users ++ configAccessor.load
  }

  def authenticate(username: String, password: String, role:UserRole): User = {

    users.get(username) match {
      case Some(user) => {
        if (user.enabled && user.role == role && md5hash(password) == user.password) {
          user
        } else {
          null
        }
      }
      case None => null
    }
  }

  def update(username: String, password: String, role: UserRole, enabled:Boolean) = {
    users.get(username) match {
      case Some(user) => {
        user.password = md5hash(password)
        user.role = role
        user.enabled = enabled
        users.synchronized {
          configAccessor.store(users)
        }
      }
      case None => throw new UserNotFoundException
    }
  }

  def create(username: String, password: String, role: UserRole) = {
    users.get(username) match {
      case Some(user) => throw new UserExistsException
      case None => {
        val user = new User(username, md5hash(password), role, true)
        users.synchronized {
          users.put(username, user)
          configAccessor.store(users)
        }
        user
      }
    }

  }

  def delete(username: String) = {
    users.get(username) match {
      case Some(user) => {
        users.synchronized {
          users.removeKey(username)
          configAccessor.store(users)
        }
      }

      case None => throw new UserNotFoundException

    }
  }

  def get(username: String): User = {
    users.get(username) match {
      case Some(user) => user
      case None => null
    }
  }

  def list: List[User] = users.values.toList


  def md5hash(input: String): String = {

    if (input == null || input.isEmpty) return ""

    val hexString = new StringBuffer

    val md = MessageDigest.getInstance("MD5")
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
}