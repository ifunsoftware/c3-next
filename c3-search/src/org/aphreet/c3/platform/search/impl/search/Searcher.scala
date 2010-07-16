/**
 * Copyright (c) 2010, Mikhail Malygin
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
 * 3. Neither the name of the IFMO nor the names of its contributors 
 * may be used to endorse or promote products derived from this software 
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.search.impl.search

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.search.SearchResultEntry
import org.apache.lucene.search._
import highlight._
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.analysis.{CachingTokenFilter, Analyzer}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.commons.logging.LogFactory

class Searcher(val indexPath: Path) {

  val log = LogFactory.getLog(getClass)

  val indexSearcher = new IndexSearcher(indexPath.file.getCanonicalPath)

  def search(sourceQuery: String): List[SearchResultEntry] = {

    val analyzer = new StandardAnalyzer

    val query = new QueryParser("contents", analyzer).parse(sourceQuery)

    log debug "query: " + query.toString

    val topDocs = indexSearcher.search(query, 30)


    var results:List[SearchResultEntry] = List()


    for (scoreDoc <- topDocs.scoreDocs) {
      val score = scoreDoc.score

      val docNum = scoreDoc.doc

      val document = indexSearcher.doc(docNum)

      val address = document.get("c3.address")
      val contents = document.get("contents")

      val fragments = fragmentsWithHighlightedTerms(analyzer, query, "contents", contents, 5, 100)

      results = new SearchResultEntry(address, score, fragments) :: results

    }

    log debug "results: " + results.toString

    results
  }

  /**
   * @author Nicholas Hrycan
   *
   */
  def fragmentsWithHighlightedTerms(analyzer:Analyzer,
                                    query:Query,
                                    fieldName:String,
                                    fieldContents:String,
                                    fragmentNumber:Int,
                                    fragmentSize:Int):Array[String] = {

    val stream = TokenSources.getTokenStream(fieldName, fieldContents, analyzer)
    val scorer = new SpanScorer(query, fieldName, new CachingTokenFilter(stream))

    val fragmenter = new SimpleSpanFragmenter(scorer, fragmentSize)

    val highlighter = new Highlighter(scorer)
    highlighter.setTextFragmenter(fragmenter)
    highlighter.setMaxDocCharsToAnalyze(java.lang.Integer.MAX_VALUE)

    highlighter.getBestFragments(stream, fieldContents, fragmentNumber)
  }


  def close {
    indexSearcher.close
  }


}