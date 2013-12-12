package org.aphreet.c3.platform.test.unit

import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.query.QueryConsumer
import org.aphreet.c3.platform.storage.RW
import org.aphreet.c3.platform.mock.StorageMock
import scala.language.reflectiveCalls
import org.aphreet.c3.platform.query.impl.QueryComponentImpl
import org.aphreet.c3.platform.storage.updater.StorageUpdater

class QueryManagerTestCase extends TestCase
{

  def testQuery(){

    val resources = Array(new Resource, new Resource, new Resource)

    val mockedIterator = new StorageIterator{

      var closed = false

      val iterator = resources.iterator

      def hasNext = iterator.hasNext

      def next() = iterator.next()

      def close() {
        closed = true
      }

      def objectsProcessed:Int = 0
    }

    val storage = new StorageMock("1", ""){

      override def mode:StorageMode = RW("")

      override def iterator(md:Map[String, String], smd:Map[String, String], filter:(Resource) => Boolean):StorageIterator
        = mockedIterator
    }

    val unavailableStorage = new StorageMock("1", ""){

      override def mode:StorageMode = U("")

    }

    trait StorageComponentMock extends StorageComponent{

      val storageManager: StorageManager = createMock(classOf[StorageManager])
      expect(storageManager.listStorages).andReturn(List(storage, unavailableStorage))
      replay(storageManager)

    }
    val queryConsumer = createMock(classOf[QueryConsumer])
    expect(queryConsumer.consume(resources(0))).andReturn(true)
    expect(queryConsumer.consume(resources(1))).andReturn(true)
    expect(queryConsumer.consume(resources(2))).andReturn(true)
    expect(queryConsumer.close())
    expect(queryConsumer.result).andReturn(null)
    replay(queryConsumer)

    val app = new Object with StorageComponentMock with QueryComponentImpl

    val queryManager = app.queryManager

    queryManager.executeQuery(Map("md_field0" -> "md_value0"), Map("smd_field0" -> "smd_value0"), queryConsumer)

    verify(app.storageManager, queryConsumer)
    assertTrue(mockedIterator.closed)
  }
}
