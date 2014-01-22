package org.aphreet.c3.platform.search

import lucene.impl.index.extractor.SimpleTextExtractor
import org.aphreet.c3.platform.search.lucene.impl.index._
import junit.framework.Assert._
import junit.framework.TestCase
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{Term, IndexWriterConfig, IndexWriter, IndexReader}
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}
import org.aphreet.c3.platform.search.lucene.impl.{SearchConfigurationAccessor, SearchConfigurationManagerImpl, SearchManagerInternal, SearchConfiguration}
import org.aphreet.c3.platform.search.lucene.SearchConfigurationManager
import org.easymock.classextension.EasyMock._
import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import org.aphreet.c3.platform.config.impl.MemoryConfigPersister
import org.apache.lucene.store.RAMDirectory
import org.aphreet.c3.platform.search.lucene.impl.SearchManagerInternal._
import org.apache.lucene.document.Document
import org.apache.lucene.analysis.Analyzer

class RamIndexerTestCase extends TestCase{

  def testResourceIndex(){

    val actorSystem = ActorSystem()

    val indexHolder = new MemoryIndexHolder

    val sender = actorSystem.actorOf(Props.create(classOf[SenderActorStub], this))

    val searchConfigurationManager = new SearchConfigurationManagerImpl(actorSystem,
      new SearchConfigurationAccessor(new MemoryConfigPersister))


    val indexer = new ParallelResourceIndexer(5, indexHolder, searchConfigurationManager, true, new SimpleTextExtractor)

    val resource = new Resource
    resource.address = "aaaaaaaaaaaaaaaaaaaa-13a34a715e9-bbbbbbbb"
    resource.systemMetadata("c3.domain.id") = "qweqweqwe"
    resource.addVersion(ResourceVersion(DataStream.create("Hello, for all c3 users!")))

    indexer.index(resource, sender)

    Thread.sleep(1000)

    val searcher = new IndexSearcher(IndexReader.open(indexHolder.directory))
    val searchQuery = new QueryParser(SearchManagerInternal.LUCENE_VERSION, "content", new StandardAnalyzer(SearchManagerInternal.LUCENE_VERSION)).parse("users")
    val topDocs = searcher.search(searchQuery, 10)

    assertEquals(1, topDocs.scoreDocs.length)
    assertEquals("aaaaaaaaaaaaaaaaaaaa-13a34a715e9-bbbbbbbb", searcher.doc(topDocs.scoreDocs(0).doc).get("c3.address"))
    assertEquals("Hello, for all c3 users!", searcher.doc(topDocs.scoreDocs(0).doc).get("content"))
  }

  class MemoryIndexHolder extends IndexHolder{

    val directory = new RAMDirectory()

    private val indexWriter = new IndexWriter(directory,
      new IndexWriterConfig(LUCENE_VERSION,
        new StandardAnalyzer(LUCENE_VERSION))
        .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))

    def addDocument(document: Document, analyzer: Analyzer){
      indexWriter.addDocument(document, analyzer)
      indexWriter.commit()
    }

    def deleteDocuments(term: Term){
      indexWriter.deleteDocuments(term)
      indexWriter.commit()
    }
  }

  class SenderActorStub extends Actor{
    def receive = {
      case _ =>
    }
  }
}
