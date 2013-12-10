package org.aphreet.c3.platform.config

import java.io.File

/**
 * Author: Mikhail Malygin
 * Date:   12/10/13
 * Time:   12:18 AM
 */
trait NullSystemDirectoryProvider extends SystemDirectoryProvider{
  def configurationDirectory: File = null

  def dataDirectory: File = null
}
