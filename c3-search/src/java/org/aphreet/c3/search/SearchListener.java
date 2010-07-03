/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * <p/>
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

package org.aphreet.c3.search;

import org.aphreet.c3.platform.access.AccessListener;
import org.aphreet.c3.platform.access.AccessManager;
import org.aphreet.c3.platform.access.JavaAccessListenerActor;
import org.aphreet.c3.platform.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class SearchListener implements AccessListener {

    private JavaAccessListenerActor actor = null;

    @Autowired
    private AccessManager accessManager;

    @PostConstruct
    public void init(){
        actor = new JavaAccessListenerActor(this);
        actor.start();
        accessManager.registerListener(actor);
    }
    
    @PreDestroy
    public void destroy(){
        accessManager.unregisterListener(actor);
        actor.stop();
    }

    @Override
    public void resourceAdded(Resource resource) {
        //Your code here
    }

    @Override
    public void resourceUpdated(Resource resource) {
        //Your code here
    }

    @Override
    public void resourceDeleted(String address) {
        //Your code here
    }
}
