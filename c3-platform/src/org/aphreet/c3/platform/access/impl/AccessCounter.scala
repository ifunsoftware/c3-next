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
package org.aphreet.c3.platform.access.impl

import actors.Actor
import actors.Actor._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.access.{ResourceAddedMsg, AccessManager}
import org.aphreet.c3.platform.common.msg.{RegisterListenerMsg, UnregisterListenerMsg, DestroyMsg}
import org.aphreet.c3.platform.statistics.{IncreaseStatisticsMsg, StatisticsManager}
import org.apache.commons.logging.LogFactory

@Component
class AccessCounter extends Actor{

  val log = LogFactory.getLog(getClass)

  var accessManager:AccessManager = _

  var statisticsManger:StatisticsManager = _

  @Autowired
  def setAccessManager(manager:AccessManager) = {accessManager = manager}

  @Autowired
  def setStatisticsManager(manager:StatisticsManager) = {statisticsManger = manager}

  {
    this.start
  }

  @PostConstruct
  def init{
    log info "Starting AccessCounter"
    accessManager ! RegisterListenerMsg(this)
  }

  @PreDestroy
  def destroy{
    log info "Stopping AccessCounter"
    accessManager ! UnregisterListenerMsg(this)
    this ! DestroyMsg
  }

  def act{
    loop{
      react{
        case ResourceAddedMsg(resource) => {
          statisticsManger ! IncreaseStatisticsMsg("c3.access.created", 1)
        }

        case DestroyMsg => this.exit
      }
    }
  }
}