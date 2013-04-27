package org.aphreet.c3.platform.metadata.impl

import org.aphreet.c3.platform.metadata.{RegisterTransientMDBuildStrategy, TransientMetadataBuildStrategy, TransientMetadataManager}
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.access.AccessManager
import org.springframework.beans.factory.annotation.Autowired
import collection.mutable
import org.aphreet.c3.platform.common.Logger
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg.DestroyMsg

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 * iFunSoftware
 */
@Component("transientMetadataManager")
class TransientMetadataManagerImpl extends TransientMetadataManager{

  private val logger = Logger(getClass)

  private val transientMetadataBuildStrategies = new mutable.HashMap[String, TransientMetadataBuildStrategy]()

  @Autowired
  var accessManager: AccessManager = _

  @PostConstruct
  def init() {
    logger.info("Starting Transient metadata manager")
    this.start()
  }

  @PreDestroy
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
