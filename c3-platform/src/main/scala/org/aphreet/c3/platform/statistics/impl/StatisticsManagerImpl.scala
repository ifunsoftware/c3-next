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
package org.aphreet.c3.platform.statistics.impl

import actors.Actor._
import collection.mutable.HashMap
import org.aphreet.c3.platform.statistics._
import org.springframework.stereotype.Component
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Qualifier

@Component("statisticsManager")
@Qualifier("StatisticsService")
class StatisticsManagerImpl extends StatisticsManager{

  val log = LogFactory.getLog(getClass)

  val statistics = new HashMap[String, Any]

  {
    log info "Starting Statistics manager"
    this.start
  }

  @PreDestroy
  def destroy{
    log info "Stopping Statistics manager"
    this ! DestroyMsg
  }

  def act{
    loop{
      react{
        case SetStatisticsMsg(key, value) =>{
          statistics.put(key, value)
        }
        case IncreaseStatisticsMsg(key, delta) => {
          statistics.get(key) match {
            case Some(string) => {
              try{
                statistics.put(key, string.asInstanceOf[Long] + delta)
              }catch{
                case e => {
                  log warn "Failed to store statistics " + key
                }
              }
            }
            case None => {
              statistics.put(key, delta)
            }
          }
        }
        case DestroyMsg => {
          log info "Statistics Manager's actor stopped"
          this.exit
        }
      }
    }
  }

  def fullStatistics:Map[String, String] = {

    Map[String, String]() ++ {
      for((key, value) <- statistics)
        yield (key, value.toString)
    }
    
  }

}