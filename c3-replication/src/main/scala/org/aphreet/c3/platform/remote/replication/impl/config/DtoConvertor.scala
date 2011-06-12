package org.aphreet.c3.platform.remote.replication.impl.config

import org.aphreet.c3.platform.storage.{StorageIndex, Storage}
import org.aphreet.c3.platform.remote.api.management.{DomainDescription, StorageIndexDescription, StorageDescription}
import org.aphreet.c3.platform.domain.{DomainMode, Domain}

/*
* Created by IntelliJ IDEA.
* User: Aphreet
* Date: 6/12/11
* Time: 7:32 PM
*/
trait DtoConvertor {

  def storageToDescription(storage:Storage):StorageDescription = {
    new StorageDescription(storage.id,
      storage.ids.toArray,
      storage.params.storageType,
      storage.path.toString,
      storage.mode.name + "(" + storage.mode.message + ")",
      storage.count,
      storage.params.indexes.map(indexToDescription(_)).toArray)
  }

  def indexToDescription(index:StorageIndex):StorageIndexDescription = {
    new StorageIndexDescription(index.name, index.multi, index.system, index.fields.toArray, index.created)
  }

  def domainFromDescription(domainDescription:DomainDescription):Domain = {
    Domain(domainDescription.id, domainDescription.name, domainDescription.key, DomainMode.byName(domainDescription.mode))
  }
}