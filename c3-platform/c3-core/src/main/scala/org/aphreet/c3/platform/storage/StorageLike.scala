package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.resource.Resource

trait StorageLike {

  def id: String

  /**
   * Add resource to storage and return address of this resource
   */
  def add(resource:Resource):String

  /**
   * Get resource from this storage
   * If resource is not found throws ResourceNotFoundException
   */
  def get(ra:String):Option[Resource]

  /**
   * Update resource that already exists in this storage
   * If there is no such resource resource gets created
   */
  def update(resource:Resource):String

  /**
   * Delete resource from storage.
   * If resource with specified address is not exists in storage
   * throws ResourceNotFoundException
   */
  def delete(ra:String)

  /**
   * Append specified map to system metadata of the specified resource
   */
  def appendMetadata(ra:String, metadata:Map[String, String], system: Boolean)

  /**
   * Get current mode
   */
  def mode:StorageMode
}