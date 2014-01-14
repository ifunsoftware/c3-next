package org.aphreet.c3.platform.metadata.impl

import collection.mutable
import org.aphreet.c3.platform.common.{ComponentLifecycle, Logger}
import org.aphreet.c3.platform.metadata.{TransientMetadataComponent, RegisterTransientMDBuildStrategy, TransientMetadataBuildStrategy, TransientMetadataManager}
import akka.actor.{Props, Actor}
import org.aphreet.c3.platform.actor.ActorComponent

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 * iFunSoftware
 */

trait TransientMetadataComponentImpl extends TransientMetadataComponent with ComponentLifecycle {

  this: ActorComponent =>

  val transientMetadataManager = new TransientMetadataManagerImpl

  class TransientMetadataManagerImpl extends TransientMetadataManager{

    private val logger = Logger(classOf[TransientMetadataComponentImpl])

    private val transientMetadataBuildStrategies = new mutable.HashMap[String, TransientMetadataBuildStrategy]()

    val async = actorSystem.actorOf(Props.create(classOf[TransientMetadataManagerActor], this))

    {
      logger.info("Starting Transient metadata manager")
    }

    class TransientMetadataManagerActor extends Actor {
      def receive = {
        case RegisterTransientMDBuildStrategy(s) => transientMetadataBuildStrategies.put(s.transientMetaField, s)
      }
    }

    def getTransientMetadata(ra: String, metaKeys: Set[String]): Map[String, String] = {
      (for {
        key <- metaKeys
        strategy <- transientMetadataBuildStrategies.get(key)
        value <- strategy.buildMetadataField(ra)
      } yield (key, value)).toMap
    }

    def supportedMetaKeys: Set[String] = transientMetadataBuildStrategies.keySet.toSet

  }
}
