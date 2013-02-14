package org.aphreet.c3.platform.metadata

import org.aphreet.c3.platform.common.WatchedActor

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 * iFunSoftware
 */
trait TransientMetadataManager extends WatchedActor{

  def getTransientMetadata(ra: String, metaKeys: Set[String]): Map[String, String]

  def supportedMetaKeys: Set[String]
}

class TransientMetadataBuildStrategy(val transientMetaField: String, mdFunc: String => Option[String]) {
  def buildMetadataField(ra: String): Option[String] = mdFunc(ra)
}

case class RegisterTransientMDBuildStrategy(s: TransientMetadataBuildStrategy)


