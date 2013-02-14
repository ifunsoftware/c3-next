package org.aphreet.c3.platform.metadata.impl

import org.aphreet.c3.platform.metadata.{RegisterTransientMDBuildStrategy, TransientMetadataBuildStrategy, MetadataManager}
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.access.AccessManager
import org.springframework.beans.factory.annotation.Autowired
import collection.mutable
import org.aphreet.c3.platform.common.WatchedActor
import javax.annotation.PostConstruct
import org.aphreet.c3.platform.common.msg.DestroyMsg

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 * iFunSoftware
 */
@Component("metadataManager")
class MetadataManagerImpl extends MetadataManager{

  private val logger = LogFactory.getLog(getClass)

  private val transientMetadataBuildStrategies = new mutable.HashMap[String, TransientMetadataBuildStrategy]()

  @Autowired
  var accessManager: AccessManager = _

  @PostConstruct
  def init() {
    logger info "Starting Metadata manager"
    this.start()
  }

  override def act() {
    loop{
      react{
        case DestroyMsg => {
          logger info "MetadataManager is stopped"
          this.exit()
        }
        case RegisterTransientMDBuildStrategy(s) => transientMetadataBuildStrategies.put(s.transientMetaField, s)
        case msg => {
          logger.error("Unknown message is received! Msg: " + msg + ". Skipping...")
        }
      }
    }
  }

  def getMetadata(ra: String): Map[String, String] = {
    accessManager.get(ra).metadata.toMap
  }

  def getTransientMetadata(ra: String, metaKeys: Set[String]): Map[String, String] = {
    (for {
      key <- metaKeys
      strategy <- transientMetadataBuildStrategies.get(key)
      value <- strategy.buildMetadataField(ra)
    } yield (key, value)).toMap
  }

  def getSystemMetadata(ra: String): Map[String, String] = {
    accessManager.get(ra).systemMetadata.toMap
  }
}