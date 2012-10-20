package org.aphreet.c3.platform.search.impl.index.extractor

import org.aphreet.c3.platform.search.impl.index.TextExtractor
import org.aphreet.c3.platform.resource.Resource

/**
 * Created with IntelliJ IDEA.
 * User: aphreet
 * Date: 10/20/12
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
class SimpleTextExtractor extends TextExtractor{

  def extract(resource: Resource) = {
    Map("content" -> resource.versions.last.data.stringValue)
  }

}
