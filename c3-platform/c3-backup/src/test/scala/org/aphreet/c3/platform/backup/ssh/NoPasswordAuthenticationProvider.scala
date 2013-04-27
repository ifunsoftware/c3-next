package org.aphreet.c3.platform.backup.ssh

import java.io.File
import java.lang.String
import com.sshtools.daemon.platform.NativeAuthenticationProvider


/**
 * User: antey
 * Date: 10.03.13
 * Time: 14:43
 */
class NoPasswordAuthenticationProvider extends NativeAuthenticationProvider {

  override def changePassword(username: String, oldpassword: String, newpassword: String): Boolean = {
    return false
  }

  override def getHomeDirectory(username: String): String = {
    return new File(System.getProperty("user.home"), "c3_int_test").getCanonicalPath
  }

  override def logoffUser {
  }

  override def logonUser(username: String, password: String): Boolean = {
    return true
  }

  override def logonUser(username: String): Boolean = {
    return true
  }
}

