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

package org.aphreet.c3.platform.access.impl

import akka.actor._
import net.sf.ehcache.{Element, Cache, CacheManager}
import org.aphreet.c3.platform.access.Constants.ACCESS_MANAGER_NAME
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.msg.RegisterNamedListenerMsg
import org.aphreet.c3.platform.common.msg.StoragePurgedMsg
import org.aphreet.c3.platform.common.msg.UnregisterNamedListenerMsg
import org.aphreet.c3.platform.common.{Logger, ComponentGuard}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.statistics.IncreaseStatisticsMsg
import scala.Some

class AccessCacheImpl(val actorSystem: ActorRefFactory, val accessMediator: ActorRef, val statisticsManager: ActorRef) extends AccessCache with ComponentGuard{

  var cache:Cache = _

  val log = Logger(getClass)

  val cacheActor = actorSystem.actorOf(Props.create(classOf[AccessCacheActor], this))

  {
    val cacheManager = CacheManager.create()

    cache = new Cache("resourceCache", 5000, false, false, 120, 120)

    cacheManager.addCache(cache)

    accessMediator ! RegisterNamedListenerMsg(cacheActor, ACCESS_MANAGER_NAME)

    log info "Access cache started"
  }

  class AccessCacheActor extends Actor {

    def receive = {
      case ResourceAddedMsg(resource, source) =>

      case ResourceDeletedMsg(address, source) =>
        remove(address)
      case ResourceUpdatedMsg(resource, source) =>
        remove(resource.address)

      case StoragePurgedMsg(source) =>
        log.info("Reseting resources cache")
        cache.removeAll()
    }

    override def postStop(){
      letItFall{
        CacheManager.getInstance.shutdown()
        accessMediator ! UnregisterNamedListenerMsg(cacheActor, ACCESS_MANAGER_NAME)
      }
      log info "AccessCache stopped"
    }
  }

  override def put(resource:Resource): Resource = {
    cache.put(new Element(resource.address, resource))
    resource
  }

  override def get(address:String):Option[Resource] = {
    val element = cache.get(address)
    if(element != null){
      statisticsManager ! IncreaseStatisticsMsg("c3.access.cache.hit", 1)
      Some(element.getObjectValue.asInstanceOf[Resource].clone)
    }else{
      statisticsManager ! IncreaseStatisticsMsg("c3.access.cache.miss", 1)
      None
    }
  }

  override def remove(address:String): String = {
    cache.remove(address)
    address
  }

}