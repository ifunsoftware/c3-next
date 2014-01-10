package org.aphreet.c3.platform.metadata

import akka.actor.ActorRef

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 * iFunSoftware
 */
trait TransientMetadataManager {

  def getTransientMetadata(ra: String, metaKeys: Set[String]): Map[String, String]

  def supportedMetaKeys: Set[String]

  def async: ActorRef
}

trait TransientMetadataComponent{

  def transientMetadataManager: TransientMetadataManager

}

class TransientMetadataBuildStrategy(val transientMetaField: String, mdFunc: String => Option[String]) {
  def buildMetadataField(ra: String): Option[String] = mdFunc(ra)
}

case class RegisterTransientMDBuildStrategy(s: TransientMetadataBuildStrategy)


