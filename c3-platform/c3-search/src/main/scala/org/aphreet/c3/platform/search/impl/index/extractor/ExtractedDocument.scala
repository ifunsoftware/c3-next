package org.aphreet.c3.platform.search.impl.index.extractor

import java.io.Reader

trait ExtractedDocument {

  def metadata: Map[String, String]

  def contentReader: Reader

  def dispose()

}
