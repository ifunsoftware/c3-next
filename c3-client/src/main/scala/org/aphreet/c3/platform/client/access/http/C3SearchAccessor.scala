package org.aphreet.c3.platform.client.access.http

import xml.{XML, NodeSeq}
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.{HttpClient, HttpStatus}
import java.net.URLEncoder

class C3SearchAccessor(val host:String, override val domain:String, override val secret:String)
  extends AbstractHttpAccessor(domain, secret){

  val requestUri = "/rest/search/"
  val url = host + requestUri

  val httpClient = new HttpClient

  def search(query:String):NodeSeq = {

    val encodedQuery = URLEncoder.encode(query, "UTF-8")

    val getMethod = new GetMethod(url + encodedQuery)

    addAuthHeader(getMethod, requestUri + encodedQuery)

    try{
      val status = httpClient.executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
          XML.loadString(getMethod.getResponseBodyAsString)
        }
        case _ =>
          println(getMethod.getResponseBodyAsString)
          throw new Exception(("Failed to get resource, code " + status).asInstanceOf[String])
      }
    }finally{
      getMethod.releaseConnection()
    }
  }

}
