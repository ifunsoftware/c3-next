package org.aphreet.c3.platform.search

import lucene.impl.index.extractor.SimpleTextExtractor
import lucene.impl.index.{RamIndexer, MergeIndexMsg}
import junit.framework.TestCase
import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}
import actors.Actor
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.IndexSearcher
import org.aphreet.c3.platform.search.lucene.impl.SearchConfiguration
import org.aphreet.c3.platform.search.lucene.impl.search.MultiFieldSearchStrategy
import org.aphreet.c3.platform.search.lucene.SearchConfigurationManager
import org.aphreet.c3.platform.search.api.SearchResultElement

abstract class AbstractSearchTestCase extends TestCase{

  var ramIndexer:RamIndexer = _

  val configuration = new SearchConfiguration
  configuration.loadFieldWeight(fieldWeights)

  val searchStrategy = new MultiFieldSearchStrategy()

  val configurationManagerStub = new SearchConfigurationManager(){
    def searchConfiguration = configuration

    def act() {}
  }

  override
  def setUp(){
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

    ramIndexer = new RamIndexer(fileIndexerMock, configurationManagerStub, 0, true, new SimpleTextExtractor)
  }

  def indexResource(resource:Resource) {
    ramIndexer.indexResource(resource)
  }

  def testSearch(){

    resources.foreach(indexResource)

    Thread.sleep(1000)

    val searcher = new IndexSearcher(IndexReader.open(ramIndexer.directory))

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

  def domain:String = "defaultDomain"

  def fieldWeights:Map[String, java.lang.Float] = Map()

  def searchQuery:String

  def resources:List[Resource]

  def verifyResults(found:List[SearchResultElement])
}
