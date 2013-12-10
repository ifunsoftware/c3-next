package org.aphreet.c3.platform.metadata.impl

import collection.mutable
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.common.{ComponentLifecycle, Logger}
import org.aphreet.c3.platform.metadata.{TransientMetadataComponent, RegisterTransientMDBuildStrategy, TransientMetadataBuildStrategy, TransientMetadataManager}

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 * iFunSoftware
 */

trait TransientMetadataComponentImpl extends TransientMetadataComponent with ComponentLifecycle {

  private val metadataManagerImpl = new TransientMetadataManagerImpl

  def transientMetadataManager: TransientMetadataManager = metadataManagerImpl

  destroy(Unit => metadataManagerImpl.destroy())

  class TransientMetadataManagerImpl extends TransientMetadataManager{

    private val logger = Logger(classOf[TransientMetadataComponentImpl])

    private val transientMetadataBuildStrategies = new mutable.HashMap[String, TransientMetadataBuildStrategy]()

    {
      logger.info("Starting Transient metadata manager")
      this.start()
    }

    def destroy() {
      logger.info("Stopping Transient metadata manager")
      this ! DestroyMsg
    }

    override def act() {
      loop{
        react{
          case DestroyMsg => {
            logger info "TransientMetadataManager is stopped"
            this.exit()
          }
          case RegisterTransientMDBuildStrategy(s) => transientMetadataBuildStrategies.put(s.transientMetaField, s)
          case msg => {
            logger.error("Unknown message is received! Msg: " + msg + ". Skipping...")
          }
        }
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
