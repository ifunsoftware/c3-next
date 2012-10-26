package org.aphreet.c3.platform.search

import ext.impl.MultiFieldSearchStrategy
import ext.{SearchResultEntry, SearchConfiguration}
import impl.index.extractor.SimpleTextExtractor
import impl.index.{RamIndexer, MergeIndexMsg}
import junit.framework.TestCase
import org.aphreet.c3.platform.resource.{DataStream, ResourceVersion, Resource}
import actors.Actor
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.IndexSearcher
import collection.JavaConversions

abstract class AbstractSearchTestCase extends TestCase{

  var ramIndexer:RamIndexer = _

  val searchConfiguration = new SearchConfiguration
  searchConfiguration.loadFieldWeight(JavaConversions.mapAsJavaMap(fieldWeights))

  val searchStrategy = new MultiFieldSearchStrategy(searchConfiguration)

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

    ramIndexer = new RamIndexer(fileIndexerMock, searchConfiguration, 0, true, new SimpleTextExtractor)
  }

  def indexResource(resource:Resource) {
    ramIndexer.indexResource(resource)
  }

  def testSearch(){

    resources.foreach(indexResource(_))

    Thread.sleep(1000)

    val searcher = new IndexSearcher(IndexReader.open(ramIndexer.directory))

    verifyResults(searchStrategy.search(searcher, searchQuery, 100, 0, domain).map(e => SearchResultElement.fromEntry(e)).toList)

  }

  def resource(address:String, data:String, metadata:Map[String, String] = Map(), domain:String = this.domain):Resource = {
    val resource = new Resource
    resource.address = address
    resource.systemMetadata.put("c3.domain.id", domain)
    resource.addVersion(ResourceVersion(DataStream.create(data)))
    resource.metadata ++= metadata

    resource
  }

  def domain:String = "defaultDomain"

  def fieldWeights:Map[String, java.lang.Integer] = Map()

  def searchQuery:String

  def resources:List[Resource]

  def verifyResults(found:List[SearchResultElement])
}
