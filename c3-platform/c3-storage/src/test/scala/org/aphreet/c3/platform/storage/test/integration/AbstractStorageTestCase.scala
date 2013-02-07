package org.aphreet.c3.platform.storage.test.integration

import junit.framework.TestCase
import java.io.File
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.storage._
import collection.mutable
import bdb.{AbstractBDBStorage, BDBStorageIterator}
import org.aphreet.c3.platform.resource.{ResourceAddress, ResourceVersion, DataStream, Resource}
import junit.framework.Assert._
import scala.Some
import java.util.Date

abstract class AbstractStorageTestCase extends TestCase{

  var testDir:File = null

  var storagePath:Path = null

  def createStorage(id:String, disableIteratorFunctionFilter:Boolean = false):Storage = {
    val params = new mutable.HashMap[String, String]()

    params.put(AbstractBDBStorage.USE_SHORT_LOCK_TIMEOUT, "true")

    if (disableIteratorFunctionFilter){
      params.put(AbstractBDBStorage.DISABLE_BDB_FUNCTION_FILTER, "true")
    }

    createStorage(id, params)
  }

  def createStorage(id:String, params:mutable.HashMap[String, String]):Storage

  override def setUp(){
    testDir = new File(System.getProperty("user.home"), "c3_int_test")
    testDir.mkdirs
    storagePath = new Path(testDir.getAbsolutePath)
  }

  override def tearDown(){
    def delDir(directory:File) {
      if(directory.isDirectory) directory.listFiles.foreach(delDir(_))
      directory.delete
    }
    delDir(testDir)
  }

  def testAdd() {

    var storage = createStorage("1000")

    try{

      val resource = createResource()

      val lengthString = resource.versions(0).data.length.toString

      val ra = storage.add(resource)

      var readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      compareResources(resource, readResource)

      resource.versions(0).systemMetadata.get(Resource.MD_DATA_LENGTH) match{
        case Some(value) => assertEquals(lengthString, value)
        case None => assertTrue("Data length sys md is not found", false)
      }

      storage.close()

      storage = createStorage("1000")

      readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      compareResources(resource, readResource)
    }finally
      storage.close()
  }

  def testAppendMetadata() {

    var storage = createStorage("1000")

    try{

      val resource = createResource()

      val ra = storage.add(resource)

      var readResource:Resource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      compareResources(resource, readResource)

      storage.close()

      storage = createStorage("1000")

      storage.appendSystemMetadata(ra, Map("sysmd1"->"sysvalue1"))

      readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      assertEquals("sysvalue1", readResource.systemMetadata.get("sysmd1").get)
      assertEquals("some_value", readResource.systemMetadata.get("key1").get)


    }finally
      storage.close()
  }

  def testVersionedUpdate() {
    val storage = createStorage("1001")

    try{

      val resource = createResource(versioned = true)
      resource.isVersioned = true

      val ra = storage.add(resource)

      //Thread.sleep(1000) //TODO Please, remove this!

      resource.metadata.put("new_key", "new_value")
      resource.systemMetadata.put("new_md_key", "new_md_value")
      resource.addVersion(createVersion())

      assertEquals("Resource must contain 2 versions", 2, resource.versions.size)

      storage.update(resource)

      val readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      compareResources(resource, readResource)

    }finally storage.close()

  }

  def testVersionedNonTrivialUpdate() {
    val storage = createStorage("1001_1")

    try{

      val resource = createResource(versioned = true)
      resource.isVersioned = true

      val ra = storage.add(resource)

      Thread.sleep(10) //Just to make sure we have another version timestamp

      resource.metadata.put("new_key", "new_value")
      resource.systemMetadata.put("new_md_key", "new_md_value")
      resource.addVersion(createVersion())

      resource.versions.foreach(_.persisted = false)

      assertEquals("Resource must contain 2 versions", 2, resource.versions.size)

      storage.update(resource)

      val readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      compareResources(resource, readResource, comparePersistedFlag = false)

    }finally storage.close()

  }

  def testVersionedNonTrivialUpdate2() {
    val storage = createStorage("1001_2")

    try{

      val resource = createResource(versioned = true)
      resource.isVersioned = true

      val ra = storage.add(resource)

      Thread.sleep(10) //Just to make sure we have another version timestamp

      val readResource1 = storage.get(ra).get
      readResource1.metadata.put("new_key", "new_value")
      readResource1.systemMetadata.put("new_md_key", "new_md_value")
      readResource1.addVersion(createVersion())

      Thread.sleep(10)

      val readResource2 = storage.get(ra).get
      readResource2.metadata.put("new_key", "new_value3")
      readResource2.systemMetadata.put("new_md_key2", "new_md_value2")
      readResource2.addVersion(createVersion("This is another data"))

      Thread.sleep(10)

      storage.update(readResource1)
      storage.update(readResource2)



      val readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      assertEquals(3, readResource.versions.size)

      val expectedMeta = resource.metadata ++ readResource2.metadata
      val expectedSystemMeta = resource.systemMetadata ++ readResource1.systemMetadata ++ readResource2.systemMetadata

      assertEquals(expectedMeta, readResource.metadata)
      assertEquals(expectedSystemMeta, readResource.systemMetadata)

      assertEquals(readResource.versions(0).data.stringValue, "This is data ")
      assertEquals(readResource.versions(1).data.stringValue, "This is new data")
      assertEquals(readResource.versions(2).data.stringValue, "This is another data")

    }finally storage.close()

  }


  def testVersionedNonTrivialUpdate3() {
    val storage = createStorage("1001_3")

    try{

      val resource = createResource(versioned = true)
      resource.isVersioned = true

      resource.calculateCheckSums

      val ra = storage.add(resource)

      Thread.sleep(10) //Just to make sure we have another version timestamp

      val readResource1 = storage.get(ra).get
      readResource1.metadata.put("new_key", "new_value")
      readResource1.systemMetadata.put("new_md_key", "new_md_value")
      readResource1.addVersion(createVersion())

      readResource1.calculateCheckSums

      Thread.sleep(10)

      val readResource2 = storage.get(ra).get
      readResource2.metadata.put("new_key", "new_value3")
      readResource2.systemMetadata.put("new_md_key2", "new_md_value2")
      readResource2.addVersion(createVersion("This is another data"))

      Thread.sleep(5)

      readResource2.addVersion(createVersion("This is fully new new data"))

      readResource2.calculateCheckSums

      Thread.sleep(10)

      storage.update(readResource1)
      storage.update(readResource2)



      val readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      assertEquals(4, readResource.versions.size)

      val expectedMeta = resource.metadata ++ readResource2.metadata
      val expectedSystemMeta = resource.systemMetadata ++ readResource1.systemMetadata ++ readResource2.systemMetadata

      assertEquals(expectedMeta, readResource.metadata)
      assertEquals(expectedSystemMeta, readResource.systemMetadata)

      assertEquals(readResource.versions(0).data.stringValue, "This is data ")
      assertEquals(readResource.versions(1).data.stringValue, "This is new data")
      assertEquals(readResource.versions(2).data.stringValue, "This is another data")
      assertEquals(readResource.versions(3).data.stringValue, "This is fully new new data")


    }finally storage.close()

  }

  def testUnversionedUpdate() {
    val storage = createStorage("1002")

    try{

      val resource = createResource()

      val ra = storage.add(resource)

      resource.metadata.put("new_key", "new_value")
      resource.systemMetadata.put("new_md_key", "new_md_value")
      resource.addVersion(createVersion())

      assertEquals("Resource must contain only one version", 1, resource.versions.size)

      storage.update(resource)

      val readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      compareResources(resource, readResource)

    }finally storage.close()
  }

  def testDelete(){

    val storage = createStorage("1003")

    try{
      val resource = createResource()

      val ra = storage.add(resource)

      val readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      assertTrue("Resource can't be null", readResource != null)

      storage.delete(ra)

      val readAfterDelete = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }

      assertTrue("Resource can't be not null after delete", readAfterDelete == null)


    }finally storage.close()

  }

  def testIterator() {
    val storage  = createStorage("1004")

    try{

      val res0 = createResource("1")
      val ra0 = storage.add(res0)

      val res1 = createResource("2")
      val ra1 = storage.add(res1)

      val res2 = createResource("3")
      val ra2 = storage.add(res2)


      val raMap = new mutable.HashMap[String, Resource]

      raMap.put(ra0, res0)
      raMap.put(ra1, res1)
      raMap.put(ra2, res2)


      val iterator = storage.iterator()
      assertEquals(0, iterator.asInstanceOf[BDBStorageIterator].secCursors.size)

      assertEquals(1, storage.asInstanceOf[AbstractBDBStorage].iterators.size)

      while(iterator.hasNext){
        val readResource = iterator.next()

        raMap.get(readResource.address) match{
          case None => assertFalse("Found resource that we did not save", false)
          case Some(r) => {
            compareResources(r, readResource)
            raMap -= r.address
          }
        }
      }

      iterator.close()

      assertEquals(0, storage.asInstanceOf[AbstractBDBStorage].iterators.size)

      assertTrue("Not all resource was accessed via iterator", raMap.size == 0)

    }finally storage.close()
  }


  def testIteratorWithOneIndex() {
    val storage  = createStorage("1008", disableIteratorFunctionFilter = true)

    storage.createIndex(new StorageIndex("pool_idx", List("pool"), system=false, multi=false, created=0l))

    try{

      val res0 = createResource("1")
      res0.metadata.put("pool", "pool0")
      val ra0 = storage.add(res0)

      val res1 = createResource("2")
      res1.metadata.put("pool", "pool1")
      storage.add(res1)

      val res2 = createResource("3")
      res2.metadata.put("pool", "pool0")
      val ra2 = storage.add(res2)


      val raMap = new mutable.HashMap[String, Resource]

      raMap.put(ra0, res0)
      raMap.put(ra2, res2)


      val iterator = storage.iterator(fields=Map("pool" -> "pool0"))

      assertEquals(1, iterator.asInstanceOf[BDBStorageIterator].secCursors.size)

      while(iterator.hasNext){
        val readResource = iterator.next()

        raMap.get(readResource.address) match{
          case None => assertFalse("Found resource that we did not save", false)
          case Some(r) => {
            compareResources(r, readResource)
            raMap -= r.address
          }
        }
      }

      iterator.close()

      assertTrue("Not all resources were accessed via iterator", raMap.size == 0)

    }finally storage.close()
  }

  def testIteratorWithOneSystemIndex() {
    val storage  = createStorage("1009", disableIteratorFunctionFilter = true)

    storage.createIndex(new StorageIndex("pool_idx", List("pool"), system=true, multi=false, created=0l))

    try{

      val res0 = createResource("1")
      res0.systemMetadata.put("pool", "pool0")
      val ra0 = storage.add(res0)

      val res1 = createResource("2")
      res1.systemMetadata.put("pool", "pool1")
      storage.add(res1)

      val res2 = createResource("3")
      res2.systemMetadata.put("pool", "pool0")
      val ra2 = storage.add(res2)


      val raMap = new mutable.HashMap[String, Resource]

      raMap.put(ra0, res0)
      raMap.put(ra2, res2)


      val iterator = storage.iterator(systemFields=Map("pool" -> "pool0"))

      assertEquals(1, iterator.asInstanceOf[BDBStorageIterator].secCursors.size)

      while(iterator.hasNext){
        val readResource = iterator.next()

        raMap.get(readResource.address) match{
          case None => assertFalse("Found resource that we did not save", false)
          case Some(r) => {
            compareResources(r, readResource)
            raMap -= r.address
          }
        }
      }

      iterator.close()

      assertTrue("Not all resources were accessed via iterator", raMap.size == 0)

    }finally storage.close()
  }

  def testIteratorWithTwoIndexes() {
    val storage  = createStorage("1010", disableIteratorFunctionFilter = true)

    storage.createIndex(new StorageIndex("pool_idx", List("pool"), system=false, multi=false, created=0l))
    storage.createIndex(new StorageIndex("pool_idx_sys", List("c3.pool"), system=true, multi=false, created=0l))

    try{

      val res0 = createResource("1")
      res0.metadata.put("pool", "pool0")
      res0.systemMetadata.put("c3.pool", "pool0")
      val ra0 = storage.add(res0)

      val res1 = createResource("2")
      res1.metadata.put("pool", "pool0")
      res1.systemMetadata.put("c3.pool", "pool1")
      storage.add(res1)

      val res2 = createResource("3")
      res2.metadata.put("pool", "pool0")
      res2.systemMetadata.put("c3.pool", "pool0")
      val ra2 = storage.add(res2)

      val res4 = createResource("4")
      res4.metadata.put("pool", "pool1")
      res4.systemMetadata.put("c3.pool", "pool0")
      storage.add(res4)

      val res5 = createResource("5")
      res5.systemMetadata.put("c3.pool", "pool0")
      storage.add(res5)


      val raMap = new mutable.HashMap[String, Resource]

      raMap.put(ra0, res0)
      raMap.put(ra2, res2)


      val iterator = storage.iterator(fields=Map("pool" -> "pool0"), systemFields=Map("c3.pool" -> "pool0"))

      assertEquals(2, iterator.asInstanceOf[BDBStorageIterator].secCursors.size)

      while(iterator.hasNext){
        val readResource = iterator.next()

        raMap.get(readResource.address) match{
          case None => assertFalse("Found resource that we did not save", false)
          case Some(r) => {
            compareResources(r, readResource)
            raMap -= r.address
          }
        }
      }

      iterator.close()

      assertTrue("Not all resources were accessed via iterator", raMap.size == 0)

    }finally storage.close()
  }

  def testIteratorWithoutIndexes() {
    val storage  = createStorage("1011")

    //storage.createIndex(new StorageIndex("pool_idx", List("pool"), system=false, multi=false, created=0l))
    //storage.createIndex(new StorageIndex("pool_idx_sys", List("c3.pool"), system=true, multi=false, created=0l))

    try{

      val res0 = createResource("1")
      res0.metadata.put("pool", "pool0")
      res0.systemMetadata.put("c3.pool", "pool0")
      val ra0 = storage.add(res0)

      val res1 = createResource("2")
      res1.metadata.put("pool", "pool0")
      res1.systemMetadata.put("c3.pool", "pool1")
      storage.add(res1)

      val res2 = createResource("3")
      res2.metadata.put("pool", "pool0")
      res2.systemMetadata.put("c3.pool", "pool0")
      val ra2 = storage.add(res2)

      val res4 = createResource("4")
      res4.metadata.put("pool", "pool1")
      res4.systemMetadata.put("c3.pool", "pool0")
      storage.add(res4)

      val res5 = createResource("5")
      res5.systemMetadata.put("c3.pool", "pool0")
      storage.add(res5)


      val raMap = new mutable.HashMap[String, Resource]

      raMap.put(ra0, res0)
      raMap.put(ra2, res2)


      val iterator = storage.iterator(fields=Map("pool" -> "pool0"), systemFields=Map("c3.pool" -> "pool0"))

      assertEquals(0, iterator.asInstanceOf[BDBStorageIterator].secCursors.size)

      while(iterator.hasNext){
        val readResource = iterator.next()

        raMap.get(readResource.address) match{
          case None => assertFalse("Found resource that we did not save", false)
          case Some(r) => {
            compareResources(r, readResource)
            raMap -= r.address
          }
        }
      }

      iterator.close()

      assertTrue("Not all resources were accessed via iterator", raMap.size == 0)

    }finally storage.close()
  }

  def testSize() {
    val storage = createStorage("1005")

    try{
      storage.add(createResource())
      println(storage.usedCapacity)
    }finally storage.close()
  }

  def testPut() {
    val storage0 = createStorage("1006")
    val storage1 = createStorage("1007")

    val resource = createResource()
    try{
      val ra = storage0.add(resource)

      val readResource = storage0.get(ra) match {
        case Some(r) => r
        case None => null
      }

      storage1.update(readResource)

      val readFrom1 = storage1.get(ra) match {
        case Some(r) => r
        case None => null
      }

      compareResources(readResource, readFrom1)

    }finally{
      storage0.close()
      storage1.close()
    }
  }

  def testLateIndexCreate(){

    val storage = createStorage("1012", disableIteratorFunctionFilter = true)

    val resource = createResource("czcxcc")
    resource.metadata.put("pool", "pool0")
    val ra = storage.add(resource)

    val resource2 = createResource("bla-bla-bla")
    resource2.metadata.put("pool", "pool1")
    val ra2 = storage.add(resource2)

    val expectedResources = new mutable.HashMap[String, Resource]()
    expectedResources.put(ra, resource)
    expectedResources.put(ra2, resource2)

    val iterator = storage.iterator(fields = Map("pool" -> "pool0"))

    verifyIteratorContents(expectedResources, iterator)

    storage.createIndex(new StorageIndex("pool_idx", List("pool"), system=false, multi=false, created=0l))

    //verifying that index has been recreated
    expectedResources.put(ra, resource)

    val iterator3 = storage.iterator(fields = Map("pool" -> "pool0"))

    verifyIteratorContents(expectedResources, iterator3)

    storage.close()
  }

  def testIterateOverCreatedField(){

    val storage = createStorage("1013", disableIteratorFunctionFilter = true)

    storage.createIndex(new StorageIndex("created_idx", List("created"), system=true, multi=false, created=0l))
    storage.createIndex(new StorageIndex("pool_idx", List("pool"), system=false, multi=false, created=0l))

    val resource1 = createResource("qweqweqwe")
    resource1.createDate = new Date(1)
    resource1.metadata.put("pool", "pool0")
    val ra1 = storage.add(resource1)

    val resource2 = createResource("qweqweqwe1")
    resource2.createDate = new Date(5)
    resource2.metadata.put("pool", "pool0")
    val ra2 = storage.add(resource2)

    val resource3 = createResource("qweqweqwe2")
    resource3.createDate = new Date(30)
    resource3.metadata.put("pool", "pool0")
    val ra3 = storage.add(resource3)


    val resource4 = createResource("qweqweqwe3")
    resource4.createDate = new Date(20)
    resource4.metadata.put("pool", "pool1")
    val ra4 = storage.add(resource4)

    val expected = new mutable.HashMap[String, Resource]()
    expected.put(ra2, resource2)
    expected.put(ra3, resource3)

    val iterator = storage.iterator(systemFields = Map("created" -> ">=5"), fields = Map("pool" -> "pool0"))

    verifyIteratorContents(expected, iterator)

    storage.close()

  }

  private def verifyIteratorContents(expectedResources:mutable.HashMap[String, Resource], iterator:StorageIterator){
    while(iterator.hasNext){
      val fetchedResource = iterator.next()

      expectedResources.get(fetchedResource.address) match {
        case Some(savedResource) => {
          compareResources(savedResource, fetchedResource)
          expectedResources.remove(fetchedResource.address)
        }
        case None => assertFalse("Found resource, that was not expected " + fetchedResource.address, true)
      }
    }

    iterator.close()

    assertTrue("Not all resources found via iterator: " + expectedResources.keySet.toList, expectedResources.isEmpty)
  }

  private def compareResources(res0:Resource, res1:Resource, comparePersistedFlag: Boolean = true) {
    assertFalse("Resource can't be null", res0 == null || res1 == null)

    assertEquals("Resource addresses do not match", res0.address, res1.address)

    assertEquals("Resource metadata do not match", res0.metadata, res1.metadata)

    assertEquals("Resource sys metadata do not match", res0.systemMetadata, res1.systemMetadata)

    assertEquals("Resource dates do not match", res0.createDate, res1.createDate)

    assertEquals("Resource versioned flag is not the same", res0.isVersioned, res1.isVersioned)

    assertEquals("Resources have different version count", res0.versions.size, res1.versions.size)

    for(i <- 0 to res0.versions.size - 1){
      val v0 = res0.versions(i)
      val v1 = res1.versions(i)

      assertEquals("Version dates do not match", v0.date, v1.date)

      assertEquals("Version metadata do not match" , v0.systemMetadata, v1.systemMetadata)

      assertEquals("Version revision do not match", v0.revision, v1.revision)

      if (comparePersistedFlag){
        assertEquals("Version persisted flag do not match", v0.persisted, v1.persisted)
      }

      assertTrue("Version datum do not match", isDatumEqual(v0.data, v1.data))
    }
  }

  private def isDatumEqual(d0:DataStream, d1:DataStream):Boolean = {

    if(d0.length != d1.length){
      println("data lengths are not equal " + d0.length + " " + d1.length )
      return false
    }

    if(d0.mimeType != d1.mimeType){
      println("data mime types are not equal " + d0.mimeType + " " + d1.mimeType)
      return false
    }

    val thisBytes = d0.getBytes
    val thatBytes = d1.getBytes

    for(i <- 0 to thatBytes.size - 1){
      if(thisBytes(i) != thatBytes(i)){
        printf("Byte streams are not equal at position %d exp: %d act: %d\n", i, thisBytes(i), thatBytes(i))
        return false
      }
    }

    true
  }

  private def createResource(data:String = "", versioned:Boolean=false):Resource = {

    val resource = new Resource
    resource.metadata.put("key", "some_value")
    resource.systemMetadata.put("key1", "some_value")
    resource.isVersioned = versioned

    val resVersion = new ResourceVersion
    resVersion.systemMetadata.put("key2", "some_other_value")

    resVersion.data = DataStream.create("This is data " + data)

    resource.addVersion(resVersion)

    resource.address = ResourceAddress.generate(resource, "12341234").stringValue

    resource
  }

  private def createVersion(data:String = "This is new data") :ResourceVersion = {
    val version = new ResourceVersion

    version.systemMetadata.put("key3", "some_value3")

    version.data = DataStream.create(data)

    version
  }

}