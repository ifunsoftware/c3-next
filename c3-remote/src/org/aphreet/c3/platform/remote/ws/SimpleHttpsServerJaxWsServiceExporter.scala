package org.aphreet.c3.platform.remote.ws

import org.springframework.remoting.jaxws.SimpleHttpServerJaxWsServiceExporter
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: Jun 22, 2010
 * Time: 5:42:26 PM
 * To change this template use File | Settings | File Templates.
 */

class SimpleHttpsServerJaxWsServiceExporter extends SimpleHttpServerJaxWsServiceExporter{

  protected var server:HttpServer = null

  protected var hostname:String = null
  protected var port:String = null

  override def setHostname(hostname:String){
    this.hostname = hostname
    super.setHostname(hostname)
  }

  override def setPort(port:String){
    this.port = port
    super.setPort(port)
  }

  override def setServer(server:HttpServer){
    this.server = server;
    super.setServer(server)
  }

  override def afterAllPropertiesSet{
    if(httpServer == null){

      var address: InetSocketAddress = (if (this.hostname != null) new InetSocketAddress(this.hostname, this.port) else new InetSocketAddress(this.port))


      setServer(HttpServer.create(address, -1))

      this.server.start

//      SSLContext sslContext =  SSLContext.getInstance("TLS");
//
//		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//
//		String ksName = "ks";
//
//		char ksPass[] = "password".toCharArray();
//		char ctPass[] = "password".toCharArray();
//
//		KeyStore ks = KeyStore.getInstance("JKS");
//		ks.load(new FileInputStream(ksName), ksPass);
//
//		kmf.init(ks, ctPass);
//
//		KeyManager[] kmList = kmf.getKeyManagers();
//
//		sslContext.init(kmList, null, null);
//
//		HttpsServer server = HttpsServer.create(new InetSocketAddress(8000), -1);
//
//		server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
//

    }

    super.afterPropertiesSet
  }

}