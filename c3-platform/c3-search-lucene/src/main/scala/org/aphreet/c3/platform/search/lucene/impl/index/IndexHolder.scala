package org.aphreet.c3.platform.search.lucene.impl.index

import org.apache.lucene.document.Document
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.Term

/**
 * Author: Mikhail Malygin
 * Date:   1/15/14
 * Time:   1:09 AM
 */
trait IndexHolder {

  def addDocument(document: Document, analyzer: Analyzer)

  def deleteDocuments(term: Term)

}
