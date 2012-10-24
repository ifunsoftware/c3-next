package org.aphreet.c3.platform.remote.servlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import org.springframework.web.context.ContextLoader
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

class CLIServlet extends HttpServlet{

  var evaluator:CLIEvaluator = null

  var accessService:PlatformAccessService = null

  var managementService:PlatformManagementService = null

  override
  def init(){
    val appContext = ContextLoader.getCurrentWebApplicationContext

    accessService = appContext.getBean(classOf[PlatformAccessService])
    managementService = appContext.getBean(classOf[PlatformManagementService])

    evaluator = appContext.getBean(classOf[CLIEvaluator])
  }

  override
  def doGet(request:HttpServletRequest, response:HttpServletResponse){

    if (evaluator != null){

      val command = request.getParameter("command")

      if (command != null) {

        response.getWriter.println(escapeHtml(evaluator.evaluate(command, accessService, managementService)))

      }else{
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
      }
    }else{
      response.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
  }

  def escapeHtml(text: String): String = {
    //We can do this more optimal however for management this is enough
    text.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("&", "&amp;")
  }
}