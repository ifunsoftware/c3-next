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

import actors.Actor
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.access._
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg._
import net.sf.ehcache.{Element, Cache, CacheManager}
import org.aphreet.c3.platform.statistics._
import org.aphreet.c3.platform.common.{ComponentGuard, WatchedActor}
import org.apache.commons.logging.LogFactory

@Component
class AccessCache extends WatchedActor with ComponentGuard{

  var cache:Cache = _

  var accessMediator:AccessMediator = _

  var statisticsManager:StatisticsManager = _

  val log = LogFactory getLog getClass

  @Autowired
  def setAccessMediator(mediator:AccessMediator) = {accessMediator = mediator}

  @Autowired
  def setStatisticsManager(manager:StatisticsManager) = {statisticsManager = manager}

  @PostConstruct
  def init{

    val cacheManager = CacheManager.create;

    cache = new Cache("resourceCache", 5000, false, false, 120, 120)

    cacheManager.addCache(cache)

    accessMediator ! RegisterNamedListenerMsg(this, AccessManagerImpl.ACCESS_MANAGER_NAME)
    this.start

    log info "Access cache started"
  }

  def act = {

    loop{
      react{
        case ResourceAddedMsg(resource, source) =>

        case ResourceDeletedMsg(address, source) =>
          this.remove(address)
        case ResourceUpdatedMsg(resource, source) =>
          this.remove(resource.address)

        case DestroyMsg =>
          letItFall{
            CacheManager.getInstance.shutdown
            accessMediator ! UnregisterNamedListenerMsg(this, AccessManagerImpl.ACCESS_MANAGER_NAME)
          }
          this.exit
      }
    }
  }

  def put(resource:Resource) = {
    cache.put(new Element(resource.address, resource))
  }

  def get(address:String):Option[Resource] = {
    val element = cache.get(address)
    if(element != null){
      statisticsManager ! IncreaseStatisticsMsg("c3.access.cache.hit", 1)
      Some(element.getObjectValue.asInstanceOf[Resource])
    }else{
      statisticsManager ! IncreaseStatisticsMsg("c3.access.cache.miss", 1)
      None
    }
  }

  def remove(address:String) = {
    cache.remove(address)
  }

  @PreDestroy
  def destroy{
    this ! DestroyMsg
  }

}