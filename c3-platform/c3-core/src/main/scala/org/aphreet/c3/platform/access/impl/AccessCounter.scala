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
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.statistics.{IncreaseStatisticsMsg, StatisticsManager}

class AccessCounter(val accessMediator: AccessMediator,
                    val statisticsManger: StatisticsManager) extends Actor{



  val log = Logger(getClass)

  {
    this.start()

    log info "Starting AccessCounter"
    accessMediator ! RegisterNamedListenerMsg(this, 'AccessCounter)
  }

  def destroy(){
    log info "Stopping AccessCounter"
    accessMediator ! UnregisterNamedListenerMsg(this, 'AccessCounter)
    this ! DestroyMsg
  }

  def act(){
    loop{
      react{
        case ResourceAddedMsg(resource, source) => {
          statisticsManger ! IncreaseStatisticsMsg("c3.access.created", 1)
        }

        case ResourceUpdatedMsg(resource, source) => {
          statisticsManger ! IncreaseStatisticsMsg("c3.access.updated", 1)
        }

        case ResourceDeletedMsg(address, source) => {
          statisticsManger ! IncreaseStatisticsMsg("c3.access.deleted", 1)
        }

        case StoragePurgedMsg(source) => {}

        case DestroyMsg =>{
          log info "AccessCounter actor stopped"
          this.exit()
        }

        case _ => log warn "Ignorring wrong message"
      }
    }
  }
}