package org.aphreet.c3.platform.metadata.impl

import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import org.aphreet.c3.platform.filesystem.{Node, FSManager}
import org.aphreet.c3.platform.resource.Resource
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import javax.annotation.PostConstruct
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.access.{AccessManager, ResourceDeletedMsg, ResourceAddedMsg}
import org.aphreet.c3.platform.metadata.{TagManager, UpdateParentTagMsg}

@Component("TagManager")
@Scope("singleton")
@Qualifier("TagManager")
class TagManagerImpl extends TagManager {

  val log = LogFactory getLog getClass

  @Autowired
  var fsManager: FSManager = _

  @Autowired
  var accessManager: AccessManager = _

  @PostConstruct
   def init() {
     log info "Starting Tag Manager"
     this.start()
   }

  override def act() {
      loop{
        react{
          case DestroyMsg => {
            log info "Tag Manager stopped"
            this.exit()
          }

          case ResourceAddedMsg(resource, source) => {

            UpdateParentTagMsg(catalog.resource.systemMetadata(Node.NODE_FIELD_PARENT), updatedTags)
          }

          case ResourceDeletedMsg(address, source) => {
           /* accessListeners.foreach {e => {
              if(e._2 != source)
                e._1 ! ResourceDeletedMsg(address, source)
              }
            } */
          }

          case UpdateParentTagMsg(resourceAddress, tags) => {
            log info "update parent tag for (" + path + " , " + domain + ")"
            val catalog = accessManager.get(resourceAddress)
            val metadata = catalog.metadata
            val updatedTags = metadata.collectionValue(Resource.MD_TAG).toSet++=tags
            metadata(Resource.MD_TAGS) = updatedTags
            accessManager.update(catalog)

            //collect statistics

            this ! UpdateParentTagMsg(catalog.resource.systemMetadata(Node.NODE_FIELD_PARENT), updatedTags)
          }
        }

      }

    }

}
