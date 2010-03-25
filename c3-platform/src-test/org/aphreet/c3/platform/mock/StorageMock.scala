package org.aphreet.c3.platform.mock

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource.{AddressGenerator, Resource}
import org.aphreet.c3.platform.storage.{RW, StorageParams, StorageIterator, Storage}

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: Mar 25, 2010
 * Time: 4:44:05 PM
 * To change this template use File | Settings | File Templates.
 */

case class StorageMock(val mockId:String, val mockPath:String) extends Storage{

  def id:String = mockId

  def add(resource:Resource):String = AddressGenerator.addressForStorage(mockId)

  def get(ra:String):Option[Resource] = null

  def update(resource:Resource):String = resource.address

  def delete(ra:String) = {}

  def put(resource:Resource) = {}



  def params:StorageParams = StorageParams(mockId, List(), path, "StorageMock", mode)

  def count:Long = 0

  def size:Long = 0

  def iterator:StorageIterator = null

  def close = {}


  def path:Path = new Path(mockPath)

  def fullPath:Path = path

  def name:String = "StorageMock-" + mockId
}