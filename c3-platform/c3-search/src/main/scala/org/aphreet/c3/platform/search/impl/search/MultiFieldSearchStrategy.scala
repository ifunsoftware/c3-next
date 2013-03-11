/*
 * Copyright (c) 2013, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES LOSS OF USE, DATA, OR PROFITS
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.search.impl.search

import collection.JavaConversions._
import collection.mutable.ArrayBuffer
import java.util
import org.apache.commons.logging.LogFactory
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.Term
import org.apache.lucene.queryParser.{ParseException, MultiFieldQueryParser}
import org.apache.lucene.search._
import highlight.{QueryScorer, SimpleSpanFragmenter, Highlighter, TokenSources}
import org.apache.lucene.util.Version.LUCENE_35
import org.aphreet.c3.platform.search.{SearchQueryException, SearchResultElement, SearchResultFragment}
import org.aphreet.c3.platform.search.impl.SearchConfiguration


class MultiFieldSearchStrategy extends SearchStrategy{

  val log = LogFactory.getLog(classOf[MultiFieldSearchStrategy])

  def search(searcher: IndexSearcher, configuration: SearchConfiguration, query: String,
             max: Int, offset: Int, domain: String): Array[SearchResultElement] = {

    val analyzer = new StandardAnalyzer(LUCENE_35)

    val russianAnalyzer = new RussianAnalyzer(LUCENE_35)
    val fieldWeights = configuration.getFieldWeights

    try {
      val parser = new MultiFieldQueryParser(LUCENE_35, fieldWeights.getFields, analyzer)
      val searchQuery = parser.parse(query)

      val topQuery = new BooleanQuery()
      topQuery.add(new BooleanClause(searchQuery, BooleanClause.Occur.MUST))
      topQuery.add(new BooleanClause(new TermQuery(new Term("domain", domain)), BooleanClause.Occur.MUST))

      if (log.isDebugEnabled) {
        log.debug("Parsed query: " + topQuery)
      }

      val termsSet = new util.HashSet[Term]()
      searchQuery.extractTerms(termsSet)

      val fieldsToSearchIn = asScalaSet(termsSet).map(term => term.field()).filter(!_.equalsIgnoreCase("domain")).toSet

      val topDocs = searcher.search(topQuery, max)

      val result = new ArrayBuffer[SearchResultElement]

      for (doc <- topDocs.scoreDocs) {

        val score = doc.score
        val num = doc.doc
        val document = searcher.doc(num)
        val address = document.get("c3.address")
        val language = document.get("lang")

        val fieldFragments = new ArrayBuffer[SearchResultFragment]

        for (field <- asScalaBuffer(document.getFields) ) {

          if (fieldsToSearchIn.contains(field.name())) {

            val content = document.get(field.name())

            val analyzerToUse = if (language == "ru") {
              russianAnalyzer
            } else {
              analyzer
            }

            val fragments = fragmentsWithHighlightedTerms(
              analyzerToUse, searchQuery, field.name(),
              content, 5, 100)

            if (fragments.length > 0) {
              fieldFragments += new SearchResultFragment(field.name(), fragments)
            }
          }


        }
        result += new SearchResultElement(address, null, score, fieldFragments.toArray)
      }

      result.toArray
    }catch{
      case e: ParseException =>
        log.warn("Incorrect search query: ", e)
        throw new SearchQueryException(e.getMessage, e)
      case e: Throwable => {
        log.error("Failed to execute query", e)
        Array()
      }
    }
  }

  def fragmentsWithHighlightedTerms(analyzer: Analyzer, query: Query, fieldName: String,
                                    fieldContent: String, fragmentNumber: Int, fragmentSize: Int): Array[String] = {

    val stream = TokenSources.getTokenStream(fieldName, fieldContent, analyzer)

    val queryScorer = new QueryScorer(query, fieldName)

    val highlighter = new Highlighter(queryScorer)

    highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, fragmentSize))
    highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE)

    highlighter.getBestFragments(stream, fieldContent, fragmentNumber)

  }
}

