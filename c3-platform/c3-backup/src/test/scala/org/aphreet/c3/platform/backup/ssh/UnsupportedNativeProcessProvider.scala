package org.aphreet.c3.platform.backup.ssh

import com.sshtools.daemon.platform.NativeProcessProvider
import com.sshtools.j2ssh.io.{DynamicBuffer}
import java.util
import java.io.{OutputStream, InputStream, IOException}

/**
 * Date:   6/26/14
 * Time:   8:00 PM
 */
class UnsupportedNativeProcessProvider extends NativeProcessProvider {


  val MESSAGE = "This server does not provide shell access, only SFTP. Goodbye.\n"

  private val stdin = new DynamicBuffer()
  private val stderr = new DynamicBuffer()
  private val stdout = new DynamicBuffer()


  def createProcess(p1: String, p2: util.Map[_, _]): Boolean = {
    true
  }


  def getDefaultTerminalProvider: String = "UnsupportedShell"


  def kill() {
    try {
      stdin.close()
    } catch {
      case e => {}
    }
    try {
      stdout.close()
    } catch {
      case e => {}
    }
    try {
      stderr.close()
    } catch {
      case e => {}
    }
  }

  def start() {
    stdin.getOutputStream.write(MESSAGE.getBytes)
  }

  def stillActive(): Boolean = {
    try {
      stdin.getInputStream.available() > 0
    } catch {
      case e: IOException => false
    }
  }

  def supportsPseudoTerminal(p1: String): Boolean = true

  def allocatePseudoTerminal(p1: String, p2: Int, p3: Int, p4: Int, p5: Int, p6: String): Boolean = true

  def waitForExitCode(): Int = 0

  def getInputStream: InputStream = stdin.getInputStream

  def getOutputStream: OutputStream = stdout.getOutputStream

  def getStderrInputStream: InputStream = stderr.getInputStream
}
