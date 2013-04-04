package org.aphreet.c3.platform.backup.ssh

import java.util.concurrent.{Callable, Executors}
import java.lang.{String, Object}
import com.sshtools.daemon.configuration.{ServerConfiguration, XmlServerConfigurationContext}
import com.sshtools.j2ssh.configuration.ConfigurationLoader
import com.sshtools.daemon.SshServer
import com.sshtools.daemon.session.SessionChannelFactory
import com.sshtools.daemon.forwarding.ForwardingServer
import java.net.{InetAddress, Socket}
import com.sshtools.j2ssh.connection.ConnectionProtocol
import org.apache.commons.logging.LogFactory
import java.io.File


/**
 * User: antey
 * Date: 10.03.13
 * Time: 14:46
 */
class SSHServerMock(var serverXmlRelativePath: String = null, var platformXmlRelativePath: String = null) {

  private val DEFAULT_SERVER_XML_RELATIVE_PATH = "ssh-server/server.xml"
  private val PLATFORM_XML_RELATIVE_PATH = "ssh-server/platform.xml"

  val log = LogFactory getLog getClass


  {
    if (serverXmlRelativePath == null) {
      serverXmlRelativePath = DEFAULT_SERVER_XML_RELATIVE_PATH
    }
    if (platformXmlRelativePath == null) {
      platformXmlRelativePath = PLATFORM_XML_RELATIVE_PATH
    }
  }

  def start {
    Executors.newSingleThreadExecutor.submit(new Callable[AnyRef] {
      override def call: AnyRef = {
        val context: XmlServerConfigurationContext = new XmlServerConfigurationContext

        context.setServerConfigurationResource(
          ConfigurationLoader.checkAndGetProperty("sshtools.server", serverXmlRelativePath))
        context.setPlatformConfigurationResource(
          System.getProperty("sshtools.platform", platformXmlRelativePath))

        ConfigurationLoader.initialize(false, context)

        val server: SshServer = new SshServer {
          def configureServices(connection: ConnectionProtocol) {
            connection.addChannelFactory(SessionChannelFactory.SESSION_CHANNEL, new SessionChannelFactory)
            if (ConfigurationLoader.isConfigurationAvailable(classOf[ServerConfiguration])) {
              if ((ConfigurationLoader.getConfiguration(classOf[ServerConfiguration]).asInstanceOf[ServerConfiguration]).getAllowTcpForwarding) {
                new ForwardingServer(connection)
              }
            }
          }

          def shutdown(msg: String) {
          }
        }

        log.info("SSH server is starting...")
        server.startServer
        return null
      }
    })
  }

  def stop {
    log.info("SSH server is shutting down...")

    val socket: Socket =
      new Socket(InetAddress.getLocalHost, (ConfigurationLoader.getConfiguration(classOf[ServerConfiguration]).asInstanceOf[ServerConfiguration]).getCommandPort)
    socket.getOutputStream.write(0x3a)
    val msg: String = "bye"
    val len: Int = if ((msg.length <= 255)) msg.length else 255
    socket.getOutputStream.write(len)
    if (len > 0) {
      socket.getOutputStream.write(msg.substring(0, len).getBytes)
    }
    socket.close
  }
}
