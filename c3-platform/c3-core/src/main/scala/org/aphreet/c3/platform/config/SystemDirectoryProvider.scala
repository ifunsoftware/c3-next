package org.aphreet.c3.platform.config

import java.io.File

/**
 * Author: Mikhail Malygin
 * Date:   12/9/13
 * Time:   4:08 PM
 */
trait SystemDirectoryProvider {

  def configurationDirectory: File

  def dataDirectory: File

}
