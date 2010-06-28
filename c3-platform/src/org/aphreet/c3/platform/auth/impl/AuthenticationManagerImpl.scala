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


import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import org.aphreet.c3.platform.auth.{ACCESS, UserRole, User, AuthenticationManager}
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct
import collection.mutable.HashMap
import com.twmacinta.util.MD5
import org.aphreet.c3.platform.exception.{UserNotFoundException, UserExistsException}

@Component("authenticationManager")
@Scope("singleton")
class AuthenticationManagerImpl extends AuthenticationManager {

  val users = new HashMap[String, User]

  var configAccessor:AuthConfigAccessor = _

  @Autowired
  def setConfigAccessor(accessor:AuthConfigAccessor) = {configAccessor = accessor}

  @PostConstruct
  def init{
    users ++ configAccessor.load
  }

  def authenticate(username:String, password:String):User = {

    users.get(username) match{
      case Some(user) => {
        if(hash(password) == user.password){
          user
        }else{
          null
        }
      }
      case None => null
    }
  }

  def update(username:String, password:String, role:UserRole) = {
    users.get(username) match {
      case Some(user) => {
        user.password = hash(password)
        user.role = role
        users.synchronized{
          configAccessor.store(users)
        }
      }
      case None => throw new UserNotFoundException
    }
  }

  def create(username:String, password:String, role:UserRole) = {
    users.get(username) match {
      case Some(user) => throw new UserExistsException
      case None => {
        val user =  new User(username, hash(password), role)
        users.synchronized{
          users.put(username, user)
          configAccessor.store(users)
        }
        user
      }
    }

  }

  def delete(username:String) = {
    users.get(username) match {
      case Some(user) => {
        users.synchronized{
          users.removeKey(username)
          configAccessor.store(users)
        }
      }

      case None => throw new UserNotFoundException

    }
  }

  def list:List[User] = users.values.toList


  private def hash(string:String):String = {
     val md5 = new MD5()
     md5.Update(string)
     md5.asHex
  }
}