package org.aphreet.c3.platform.search.lucene.impl.index.extractor

trait ExtractedDocument {

  def metadata: Map[String, String]

  def content: String

  def dispose()

}
