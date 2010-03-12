package org.aphreet.c3.platform.storage.test.integration

import junit.framework.TestCase
import junit.framework.Assert._

import scala.collection.mutable.HashMap

import org.aphreet.c3.platform.storage._
import common.BDBConfig
import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.common._

import java.io.File

import org.aphreet.c3.platform.storage.bdb.PureBDBStorage


class PureBDBStorageTest extends TestCase{

  var testDir:File = null
  
  var storagePath:Path = null
  
  def createStorage(id:String):Storage = 
    new PureBDBStorage(id, storagePath, new BDBConfig(true, true, 20))
 
  override def setUp{
	testDir = new File(System.getProperty("user.home"), "c3_int_test")
    testDir.mkdirs
    storagePath = new Path(testDir.getAbsolutePath)
  }
  
  override def tearDown{
    def delDir(directory:File) {
      if(directory.isDirectory) directory.listFiles.foreach(delDir(_))
      directory.delete
    }
    delDir(testDir)
  }
  
  def testAdd = {
    
    var storage = createStorage("1000")
    
    try{
      
      val resource = createResource
      
      val ra = storage.add(resource)
      
      var readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }
      
      compareResources(resource, readResource)
      
      storage.close
      
      storage = createStorage("1000")
      
      readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }
      
      compareResources(resource, readResource)
    }finally
      storage.close
  }
  
  def testVersionedUpdate {
    var storage = createStorage("1001")
    
    try{
      
      val resource = createResource
      resource.isVersioned = true
      
      val ra = storage.add(resource)
      
      resource.metadata.put("new_key", "new_value")
      resource.systemMetadata.put("new_md_key", "new_md_value")
      resource.addVersion(createVersion)
      
      assertEquals("Resource must contain 2 versions", 2, resource.versions.size)
      
      storage.update(resource)
      
      val readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }
      
      compareResources(resource, readResource)
      
    }finally storage.close
    
  }
  
  def testUnversionedUpdate {
    var storage = createStorage("1002")
    
    try{
      
      val resource = createResource
      resource.isVersioned = false
      
      val ra = storage.add(resource)
      
      resource.metadata.put("new_key", "new_value")
      resource.systemMetadata.put("new_md_key", "new_md_value")
      resource.addVersion(createVersion)
      
      assertEquals("Resource must contain only one version", 1, resource.versions.size)
      
      storage.update(resource)
      
      val readResource = storage.get(ra) match {
        case Some(r) => r
        case None => null
      }
      
      compareResources(resource, readResource)
      
    }finally storage.close
  }
  
  def testDelete{
    
    val storage = createStorage("1003")
    
    try{
      val resource = createResource
      
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
      
      
    }finally storage.close
    
  }
  
  def testIterator = {
    val storage  = createStorage("1004")
    
    try{
      
      val res0 = createResource
      val ra0 = storage.add(res0)
      
      val res1 = createResource
      val ra1 = storage.add(res1)
      
      val res2 = createResource
      val ra2 = storage.add(res2)
      
      
      val raMap = new HashMap[String, Resource]
      
      raMap.put(ra0, res0)
      raMap.put(ra1, res1)
      raMap.put(ra2, res2)
      
      
      val iterator = storage.iterator
      
      while(iterator.hasNext){
        val readResource = iterator.next
        
        raMap.get(readResource.address) match{
          case None => assertFalse("Found resource that we did not save", false)
          case Some(r) => {
            compareResources(r, readResource)
            raMap -= r.address
          }
        }
      }
      
      iterator.close
      
      assertTrue("Not all resource was accessed via iterator", raMap.size == 0)
      
    }finally storage.close
  }
  
  def testSize = {
    val storage = createStorage("1005")
    
    try{
      storage.add(createResource)
      println(storage.size)
    }finally storage.close
  }
  
  def testPut = {
    val storage0 = createStorage("1006")
    val storage1 = createStorage("1007")
    
    val resource = createResource
    try{   
      val ra = storage0.add(resource)
    
      val readResource = storage0.get(ra) match {
        case Some(r) => r
        case None => null
      }
    
      storage1.put(readResource)
      
      val readFrom1 = storage1.get(ra) match {
        case Some(r) => r
        case None => null
      }
      
      compareResources(readResource, readFrom1)
      
    }finally{
      storage0.close
      storage1.close
    }
    
  }
  
  private def compareResources(res0:Resource, res1:Resource) = {
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
      
      assertEquals("Version persisted flag do not match", v0.persisted, v1.persisted)
      
      assertTrue("Version datum do not match", isDatumEqual(v0.data, v1.data))
    }
  }
  
  private def isDatumEqual(d0:DataWrapper, d1:DataWrapper):Boolean = {
    
    if(d0.length != d1.length){
      println("datum lengths are not equal " + d0.length + " " + d1.length )
      return false
    }
    
    if(d0.mimeType != d1.mimeType){
      println("datum mime types are not equal " + d0.mimeType + " " + d1.mimeType)
      return false
    }
        
    val thisBytes = d0.getBytes
    val thatBytes = d1.getBytes
    
    for(i <- 0 to thatBytes.size - 1){
      if(thisBytes(i) != thatBytes(i)){
        println("Byte streams are not equal")
        return false
      }
    }
    
    true
  }
  
  private def createResource:Resource = {
    val resource = new Resource
    resource.metadata.put("key", "some_value")
    resource.systemMetadata.put("key1", "some_value")
    resource.isVersioned = true
      
    val resVersion = new ResourceVersion
    resVersion.systemMetadata.put("key2", "some_other_value")
      
    resVersion.data = DataWrapper.wrap("This is data")
      
    resource.addVersion(resVersion)
    
    resource
  }
  
  private def createVersion:ResourceVersion = {
    val version = new ResourceVersion
    
    version.systemMetadata.put("key3", "some_value3")
    
    version.data = DataWrapper.wrap("This is new data")
    
    version
  }
 
}
