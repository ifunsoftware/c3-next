/*
 * Copyright (c) 2013, Mikhail Malygin
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
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
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

package org.aphreet.c3.platform.remote.replication.impl.data.stats

import akka.actor.{Props, ActorRefFactory, Actor}
import collection.mutable
import org.aphreet.c3.platform.statistics.{SetStatisticsMsg, StatisticsManager}
import org.aphreet.c3.platform.common.ActorRefHolder

case class Delay(time: Long) {

  var value = 0L

  var points = 0

  def update(delay: Long) {
    value = (value * points + delay)/(points + 1)
    points = points + 1
  }
}

class DelayHistory(val actorSystem: ActorRefFactory, val statisticsManager: StatisticsManager) extends ActorRefHolder{

  var history: mutable.Queue[Delay] = mutable.Queue(new Delay(roundTime(0)))

  val async = actorSystem.actorOf(Props.create(classOf[DelayHistoryActor], this))

  def add(delay: Long, timestamp: Long){
    val time = roundTime(timestamp)

    if(history.last.time != time){
      append(time)
    }

    history.last.update(delay)
  }

  class DelayHistoryActor extends Actor {
    def receive = {
      case DelayInfoMsg(timestamp, delay) => add(delay, timestamp)
    }
  }

  private def roundTime(time: Long): Long = time / 1000

  private def append(time: Long){
    history.enqueue(new Delay(time))
    while(time - history.last.time > 10 * 60){
      history.dequeue()
    }

    val currentTimestamp = System.currentTimeMillis()

    if(statisticsManager != null){
      statisticsManager ! SetStatisticsMsg("c3.replication.delay.5sec", averageDelay(5, currentTimestamp).toString)
      statisticsManager ! SetStatisticsMsg("c3.replication.delay.1min", averageDelay(60, currentTimestamp).toString)
      statisticsManager ! SetStatisticsMsg("c3.replication.delay.10min", averageDelay(600, currentTimestamp).toString)
    }
  }

  def averageDelay(period: Int, beforeTime: Long): Long = {

    val time = roundTime(beforeTime)

    val elements = history.filter(_.time >= time - period + 1)

    if (!elements.isEmpty)
      elements.foldRight(0L)(_.value + _) /elements.size
    else
      0L
  }
}

case class DelayInfoMsg(timestamp: Long, delay: Long)

