package org.aphreet.c3.platform.search

import actors.Actor
import impl.index.extractor.SimpleTextExtractor
import impl.index.{IndexMsg, MergeIndexMsg, RamIndexer}
import junit.framework.Assert._
import junit.framework.TestCase
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexReader
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}
import org.aphreet.c3.platform.search.impl.{SearchManagerInternal, SearchConfiguration}

class RamIndexerTestCase extends TestCase{

  def testResourceIndex(){

    val fileIndexerMock = new Actor{

      override def act(){
        loop{
          react{
            case MergeIndexMsg(directory) =>
            case _ =>
          }
        }
      }
    }

    fileIndexerMock.start()

    val ramIndexer = new RamIndexer(fileIndexerMock, new SearchConfigurationManager {
      def searchConfiguration = new SearchConfiguration

      def act() {}
    }, 0, true, new SimpleTextExtractor)
    ramIndexer.start()

    val resource = new Resource
    resource.address = "aaaaaaaaaaaaaaaaaaaa-13a34a715e9-bbbbbbbb"
    resource.systemMetadata("c3.domain.id") = "qweqweqwe"
    resource.addVersion(ResourceVersion(DataStream.create("Hello, for all c3 users!")))

    ramIndexer ! IndexMsg(resource)

    Thread.sleep(1000)

    val searcher = new IndexSearcher(IndexReader.open(ramIndexer.directory))
    val searchQuery = new QueryParser(SearchManagerInternal.LUCENE_VERSION, "content", new StandardAnalyzer(SearchManagerInternal.LUCENE_VERSION)).parse("users")
    val topDocs = searcher.search(searchQuery, 10)

    assertEquals(1, topDocs.scoreDocs.length)
    assertEquals("aaaaaaaaaaaaaaaaaaaa-13a34a715e9-bbbbbbbb", searcher.doc(topDocs.scoreDocs(0).doc).get("c3.address"))
    assertEquals("Hello, for all c3 users!", searcher.doc(topDocs.scoreDocs(0).doc).get("content"))
  }
}
