package org.aphreet.c3.platform.storage.updater

trait StorageUpdater {

  def applyTransformation(transformation: Transformation)

}

trait StorageUpdaterComponent {

  def storageUpdater: StorageUpdater

}