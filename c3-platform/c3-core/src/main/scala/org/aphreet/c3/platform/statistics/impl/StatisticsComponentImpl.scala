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

import collection.mutable
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.common.{ComponentLifecycle, Logger}
import org.aphreet.c3.platform.statistics._
import akka.actor.{Props, Actor}
import org.aphreet.c3.platform.actor.ActorComponent

trait StatisticsComponentImpl extends StatisticsComponent with ComponentLifecycle{

  this: ActorComponent =>

  val statisticsManager = new StatisticsManagerImpl

  class StatisticsManagerImpl extends StatisticsManager {

    val log = Logger(classOf[StatisticsComponentImpl])

    private val statistics = new mutable.HashMap[String, Any]

    val async = actorSystem.actorOf(Props[StatisticsActor])

    {
      log info "Starting StatisticsManager"
    }

    def fullStatistics:Map[String, String] = {
      Map[String, String]() ++ {
        for((key, value) <- statistics)
        yield (key, value.toString)
      }
    }

    class StatisticsActor extends Actor {
      override def receive = {
        case SetStatisticsMsg(key, value) =>{
          statistics.put(key, value)
        }
        case IncreaseStatisticsMsg(key, delta) => {
          statistics.get(key) match {
            case Some(string) => {
              try{
                statistics.put(key, string.asInstanceOf[Long] + delta)
              }catch{
                case e: Throwable => {
                  log warn "Failed to store statistics " + key
                }
              }
            }
            case None => {
              statistics.put(key, delta)
            }
          }
        }

        case ResetStatisticsMsg(key) => {
          statistics.remove(key)
        }
      }
    }
  }
}