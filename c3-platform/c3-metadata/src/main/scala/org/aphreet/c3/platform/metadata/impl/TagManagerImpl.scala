  package org.aphreet.c3.platform.metadata.impl

  import org.apache.commons.logging.LogFactory
  import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
  import org.aphreet.c3.platform.filesystem.{Node, FSManager}
  import org.aphreet.c3.platform.resource.{MetadataHelper, Resource}
  import org.springframework.stereotype.Component
  import org.springframework.context.annotation.Scope
  import javax.annotation.PostConstruct
  import org.aphreet.c3.platform.common.msg.DestroyMsg
  import org.aphreet.c3.platform.access.{ResourceUpdatedMsg, ResourceOwner, AccessManager, ResourceAddedMsg}
  import org.aphreet.c3.platform.metadata.{AddParentTagMsg, DeleteParentTagMsg, TagManager}
  import scala.None
  import scala.collection.Map

  @Component("TagManager")
  @Scope("singleton")
  @Qualifier("TagManager")
  class TagManagerImpl extends TagManager with ResourceOwner {

    val log = LogFactory getLog getClass

    @Autowired
    var accessManager: AccessManager = _

    @PostConstruct
     def init() {
       log info "Starting Tag Manager"
       this.start()
     }

    override def deleteResource(resource:Resource) {
      val metadata = resource.metadata
      val tagsString: Option[String] = metadata(Resource.MD_TAGS)
      tagsString match {
        case Some(tagsString) =>
          val updatedTags:Map[String, Int] = MetadataHelper.parseTagMap(tagsString)
          this ! DeleteParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), updatedTags)
      }
    }

    override def act() {
        loop{
          react{
            case DestroyMsg => {
              log info "Tag Manager stopped"
              this.exit()
            }

            case ResourceAddedMsg(resource, source) => {
              val metadata = resource.metadata
              val tagsString: Option[String] = metadata(Resource.MD_TAGS)
              tagsString match {
                 case Some(tagsString) =>
                   val addedTags:Map[String, Int] = MetadataHelper.parseTagMap(tagsString)
                   this ! AddParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), addedTags)
               }
            }

            case ResourceUpdatedMsg(resource, source) => {
              val metadata = resource.metadata
              val tagsString: Option[String] = metadata(Resource.MD_TAGS)
              //TODO find difference between (parentTags - otherChildrenTags) and current resource tags
            }

            case DeleteParentTagMsg(resourceAddress, tags) => {
              resourceAddress match {
                case None =>
                  log error "got empty resourceAddress"
                case Some(resourceAddress) =>
                  val catalog = accessManager.get(resourceAddress)
                  val metadata = catalog.metadata
                  val tagsString: Option[String] = metadata(Resource.MD_TAGS)
                  tagsString match {
                    case Some(tagsString) =>
                      //collect statistics
                      val tagsBeforeDelete:Map[String, Int] = MetadataHelper.parseTagMap(tagsString)
                     //delete tags
                      val updatedTags = tagsBeforeDelete.map(tagInfo => {
                        if (tags.contains(tagInfo._1)) (tagInfo._1, tagInfo._2 - tags.get(tagInfo._1).get)
                        else tagInfo}).filter(tagInfo => tagInfo._2 > 0)

                      metadata(Resource.MD_TAGS) = MetadataHelper.writeTagMap(updatedTags)
                      accessManager.update(catalog)

                      catalog.systemMetadata(Node.NODE_FIELD_PARENT) foreach {
                        address => this ! DeleteParentTagMsg(Some(address), tags)
                      }

                    case None =>
                      log info "nothing to delete (" + resourceAddress + ")"
                  }
              }
            }

            case AddParentTagMsg(resourceAddress, tags) => {
                resourceAddress match {
                  case None =>
                    log error "got empty resourceAddress"
                  case Some(resourceAddress) =>

                    val catalog = accessManager.get(resourceAddress)
                    val metadata = catalog.metadata
                    val tagsString: Option[String] = metadata(Resource.MD_TAGS)
                    tagsString match {
                      case Some(tagsString) =>
                       //collect statistics
                       val tagsBeforeAdd:Map[String, Int] = MetadataHelper.parseTagMap(tagsString)
                      //delete tags
                       val updatedTags = tagsBeforeAdd.map(tagInfo => {
                         if (tags.contains(tagInfo._1)) (tagInfo._1, tagInfo._2 + tags.get(tagInfo._1).get)
                         else tagInfo})

                       metadata(Resource.MD_TAGS) = MetadataHelper.writeTagMap(updatedTags)
                       accessManager.update(catalog)

                       catalog.systemMetadata(Node.NODE_FIELD_PARENT) foreach {
                          address => this ! AddParentTagMsg(Some(address), tags)
                       }

                      case None =>
                        log info "nothing to add (" + resourceAddress + ")"
                    }
                }

              }
          }

        }

      }
  }
