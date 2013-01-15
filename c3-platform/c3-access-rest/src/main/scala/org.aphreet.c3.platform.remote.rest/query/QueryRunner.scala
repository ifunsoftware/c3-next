package org.aphreet.c3.platform.remote.rest.query

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.aphreet.c3.platform.accesscontrol.READ
import org.aphreet.c3.platform.query.QueryManager
import org.aphreet.c3.platform.remote.rest.controllers.RestController
import scala.collection.mutable

trait QueryRunner extends RestController {

  private val SYSTEM_META = "system."

  def queryManager: QueryManager

  def executeQuery(req: HttpServletRequest,
                               resp: HttpServletResponse,
                               contentType: String,
                               limit: Option[Int],
                               offset: Int) {
      val accessTokens = getAccessTokens(READ, req)

      var actualLimit = limit
      var actualOffset = offset

      val userMetaMap = new mutable.HashMap[String, String]
      val systemMetaMap = new mutable.HashMap[String, String]

      val enum = req.getParameterNames

      while (enum.hasMoreElements) {
        val key: String = enum.nextElement.asInstanceOf[String]
        val value: String = req.getParameter(key)

        key match {
          case "limit" => actualLimit = Some(value.toInt)
          case "offset" => actualOffset = value.toInt
          case _ => {
            if (key.startsWith(SYSTEM_META))
              systemMetaMap.put(key.replace(SYSTEM_META, ""), value)
            else
              userMetaMap.put(key, value)
          }
        }
      }

      val consumer = new RestQueryConsumer(resp.getWriter, getResultWriter(contentType), actualOffset, actualLimit)

      queryManager.executeQuery(userMetaMap.toMap, accessTokens.metadataRestrictions ++ systemMetaMap.toMap, consumer)

      resp.flushBuffer()
    }
}
