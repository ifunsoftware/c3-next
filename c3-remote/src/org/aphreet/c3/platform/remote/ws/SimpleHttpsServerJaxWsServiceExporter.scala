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
package org.aphreet.c3.platform.remote.ws

import org.springframework.remoting.jaxws.SimpleHttpServerJaxWsServiceExporter
import java.net.InetSocketAddress
import java.security.KeyStore
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import com.sun.net.httpserver.{HttpsConfigurator, HttpsServer, HttpServer}
import javax.xml.ws.Endpoint
import javax.jws.WebService
import reflect.BeanProperty
import java.io.FileInputStream

class SimpleHttpsServerJaxWsServiceExporter extends SimpleHttpServerJaxWsServiceExporter{

  @BeanProperty
  protected var keyStoreName = ".keystore"

  @BeanProperty
  protected var keyStorePassword = "password"

  /*
    Overriding superclass properties
   */
  protected var server:HttpServer = null

  protected var hostname:String = null

  protected var port:Int = 9443


  override def setHostname(hostname:String){
    this.hostname = hostname
    super.setHostname(hostname)
  }

  override def setPort(port:Int){
    this.port = port
    super.setPort(port)
  }

  override def setServer(server:HttpServer){
    this.server = server;
    super.setServer(server)
  }

  override def afterPropertiesSet{
    if(server == null){

      var address: InetSocketAddress = (if (this.hostname != null) new InetSocketAddress(this.hostname, this.port) else new InetSocketAddress(this.port))


      val sslContext = SSLContext.getInstance("TLS")

      val kmf:KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")

      val ksPass = keyStorePassword.toCharArray
      val ctPass = keyStorePassword.toCharArray

      val keyStore = KeyStore.getInstance("JKS")
      keyStore.load(new FileInputStream(keyStoreName), ksPass)

      kmf.init(keyStore, ctPass)

      val keyManagerList = kmf.getKeyManagers

      sslContext.init(keyManagerList, null, null)


      val httpsServer = HttpsServer.create(address, -1)
      httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext))

      setServer(httpsServer)

      httpsServer.start

    }

    super.afterPropertiesSet
  }

  override protected def publishEndpoint(endpoint:Endpoint, annotation:WebService){
    try{
      super.publishEndpoint(endpoint, annotation)
    }catch{
      case e =>{
        this.destroy
        throw e
      }
    }
  }

}