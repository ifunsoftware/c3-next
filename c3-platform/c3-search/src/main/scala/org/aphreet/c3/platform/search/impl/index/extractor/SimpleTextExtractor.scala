package org.aphreet.c3.platform.search.impl.index.extractor

import org.aphreet.c3.platform.search.impl.index.TextExtractor
import org.aphreet.c3.platform.resource.Resource
import java.io.StringReader

class SimpleTextExtractor extends TextExtractor{

  def extract(resource: Resource): Option[ExtractedDocument] = Some(new SimpleExtractedDocument(resource))

}

class SimpleExtractedDocument(val resource: Resource) extends ExtractedDocument{

  def metadata = Map()

  lazy val content = new String(resource.versions.last.data.getBytes, "UTF-8")

  def dispose() {

  }

}
