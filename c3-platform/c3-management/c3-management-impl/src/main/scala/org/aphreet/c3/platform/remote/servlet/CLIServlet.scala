package org.aphreet.c3.platform.remote.servlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import org.springframework.web.context.ContextLoader
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.remote.api.{RemoteException, CLIEvaluator}

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
        try{
          response.getWriter.println(escapeHtml(evaluator.evaluate(command, accessService, managementService)))
        }catch{
          case e: RemoteException => response.getWriter.println(e.getMessage)
        }
      }else{
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
      }
    }else{
      response.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
  }

  def escapeHtml(text: String): String = {
    //We can do this some another nice way however it is enough for management
    text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
  }
}