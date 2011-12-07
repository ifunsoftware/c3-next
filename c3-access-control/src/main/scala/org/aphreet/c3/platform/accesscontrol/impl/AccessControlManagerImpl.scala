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
package org.aphreet.c3.platform.accesscontrol.impl

import org.aphreet.c3.platform.accesscontrol.{Action, ResourceAccessChecker, AccessControlManager}
import org.aphreet.c3.platform.resource.Resource
import collection.mutable.HashSet
import org.springframework.stereotype.Component
import org.apache.commons.logging.LogFactory
import javax.annotation.PostConstruct

@Component("accessControlManager")
class AccessControlManagerImpl extends AccessControlManager {

  val log = LogFactory.getLog(getClass)

  val checkers = new HashSet[ResourceAccessChecker]

  @PostConstruct
  def init(){
    log.info("Starting access control manager")
  }

  def registerChecker(checker:ResourceAccessChecker) {
    this.synchronized{
      
      log.debug("Registering checker " + checker.toString)
      
      checkers += checker
    }
  }

  def unregisterChecker(checker:ResourceAccessChecker) {
    this.synchronized{

      log.debug("Unregistering checker " + checker.toString)

      checkers -= checker
    }
  }

  def canPerformActionWithResource(action:Action, resource:Resource, accessParams:Map[String, String]):Boolean = {
    checkers.forall(c => c.canPerformActionWithResource(action, resource, accessParams))
  }
}