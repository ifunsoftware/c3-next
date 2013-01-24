package org.aphreet.c3.platform.search.impl.index.extractor

trait ExtractedDocument {

  def metadata: Map[String, String]

  def content: String

  def dispose()

}
