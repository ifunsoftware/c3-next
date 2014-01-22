package org.aphreet.c3.platform.search

import akka.actor.{Actor, Props, ActorSystem}
import junit.framework.TestCase
import lucene.impl.index.extractor.SimpleTextExtractor
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.{IndexWriterConfig, IndexWriter, Term, IndexReader}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.RAMDirectory
import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}
import org.aphreet.c3.platform.search.api.SearchResultElement
import org.aphreet.c3.platform.search.lucene.SearchConfigurationManager
import org.aphreet.c3.platform.search.lucene.impl.SearchConfiguration
import org.aphreet.c3.platform.search.lucene.impl.SearchManagerInternal._
import org.aphreet.c3.platform.search.lucene.impl.index.{IndexHolder, ParallelResourceIndexer}
import org.aphreet.c3.platform.search.lucene.impl.search.MultiFieldSearchStrategy

abstract class AbstractSearchTestCase extends TestCase{

  val actorSystem = ActorSystem()

  val senderMock = actorSystem.actorOf(Props.create(classOf[SenderActorStub], this))

  var indexer: ParallelResourceIndexer = _

  val configuration = new SearchConfiguration
  configuration.loadFieldWeight(fieldWeights)

  val indexHolder = new MemoryIndexHolder

  val searchStrategy = new MultiFieldSearchStrategy()

  val configurationManagerStub = new SearchConfigurationManager {
    def async = actorSystem.actorOf(Props.create(classOf[ConfigurationManagerActorStub], this))

    def searchConfiguration = configuration

    class ConfigurationManagerActorStub extends Actor{
      def receive = {
        case _ =>
      }
    }
  }

  override
  def setUp(){
    indexer = new ParallelResourceIndexer(5, indexHolder, configurationManagerStub, true, new SimpleTextExtractor)
  }

  def indexResource(resource:Resource) {
    indexer.index(resource, senderMock)
  }

  def testSearch(){

    resources.foreach(indexResource)

    Thread.sleep(1000)

    val searcher = new IndexSearcher(IndexReader.open(indexHolder.directory))

    verifyResults(searchStrategy.search(searcher, configuration, searchQuery, 100, 0, domain).elements.toList)
  }

  def resource(address:String, data:String, metadata:Map[String, String] = Map(), domain:String = this.domain):Resource = {
    val resource = new Resource
    resource.address = address
    resource.systemMetadata("c3.domain.id") = domain
    resource.addVersion(ResourceVersion(DataStream.create(data)))
    resource.metadata ++= metadata

    resource
  }

  override def tearDown(){
    actorSystem.shutdown()
    actorSystem.awaitTermination()

    indexer.destroy()
  }

  def domain:String = "defaultDomain"

  def fieldWeights:Map[String, java.lang.Float] = Map()

  def searchQuery:String

  def resources:List[Resource]

  def verifyResults(found:List[SearchResultElement])

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
