package org.aphreet.c3.platform.metadata.impl

import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import org.aphreet.c3.platform.filesystem.{Directory, Node, FSManager}
import org.aphreet.c3.platform.resource.{MetadataHelper, Resource}
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import javax.annotation.PostConstruct
import org.aphreet.c3.platform.common.msg.{RegisterNamedListenerMsg, DestroyMsg}
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.metadata._
import scala.None
import scala.collection.{mutable, Map}
import org.aphreet.c3.platform.metadata.AddParentTagMsg
import org.aphreet.c3.platform.access.ResourceUpdatedMsg
import org.aphreet.c3.platform.common.msg.RegisterNamedListenerMsg
import org.aphreet.c3.platform.metadata.DeleteParentTagMsg
import org.aphreet.c3.platform.access.ResourceAddedMsg
import scala.Some
import scala.Predef._
import org.aphreet.c3.platform.access.ResourceUpdatedMsg
import org.aphreet.c3.platform.common.msg.RegisterNamedListenerMsg
import org.aphreet.c3.platform.metadata.DeleteParentTagMsg
import org.aphreet.c3.platform.metadata.RebuildParentTagMsg
import org.aphreet.c3.platform.access.ResourceAddedMsg
import scala.Some
import org.aphreet.c3.platform.metadata.AddParentTagMsg
import org.aphreet.c3.platform.access.ResourceUpdatedMsg
import org.aphreet.c3.platform.common.msg.RegisterNamedListenerMsg
import org.aphreet.c3.platform.metadata.DeleteParentTagMsg
import org.aphreet.c3.platform.metadata.RebuildParentTagMsg
import org.aphreet.c3.platform.access.ResourceAddedMsg
import scala.Some
import org.aphreet.c3.platform.metadata.AddParentTagMsg

@Component("TagManager")
@Scope("singleton")
@Qualifier("TagManager")
class TagManagerImpl extends TagManager with ResourceOwner {

  val log = LogFactory getLog getClass

  @Autowired
  var accessManager: AccessManager = _

  @Autowired
  var accessMediator: AccessMediator =_

  @PostConstruct
  def init() {
    log info "Starting Tag Manager"
    accessMediator ! RegisterNamedListenerMsg(this, 'tagManager)
    this.start()
  }

  override def deleteResource(resource:Resource) {
    val metadata = resource.metadata

    metadata(Resource.MD_TAGS) match {
      case Some(tagsString) =>
        val node:Node = Node.fromResource(resource)

        if (node.isDirectory) {
          val deletedTags:Map[String, Int] = MetadataHelper.parseTagMap(tagsString, (tagInfo: String) => {
            (tagInfo.split(":")(0), Int.unbox(tagInfo.split(":")(1)))
          }).toMap[String, Int]

          this ! DeleteParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), deletedTags)
        } else {
          val tagMap = new mutable.HashMap[String, Int]
          val tagCollection = metadata.collectionValue(Resource.MD_TAGS)

          tagCollection.foreach(childTag => {
            tagMap.put(childTag, 1)})

          this ! DeleteParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), tagMap)
        }
    }
  }

  override def act() {
    loop{
      react{
        case DestroyMsg => {
          log info "tag manager stopped"
          this.exit()
        }

        case ResourceAddedMsg(resource, source) => {
          val metadata = resource.metadata
          val tagsString: Option[String] = metadata(Resource.MD_TAGS)
          tagsString match {
            case Some(tagsS) =>
              log info "ResourceAddedMsg: " + resource.address
              val node:Node = Node.fromResource(resource)

              if (node.isDirectory) {
                val addedTags:Map[String, Int] = MetadataHelper.parseTagMap(tagsS, (tagInfo: String) => {
                            (tagInfo.split(":")(0), Int.unbox(tagInfo.split(":")(1)))
                          }).toMap[String, Int]
                this ! AddParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), addedTags)
              } else {
                val tagMap = new mutable.HashMap[String, Int]
                val tagCollection = metadata.collectionValue(Resource.MD_TAGS)

                tagCollection.foreach(childTag => {
                  tagMap.put(childTag, 1)})

                this ! AddParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), tagMap)
              }
          }
        }

        case ResourceUpdatedMsg(resource, source) => {
          val parentAddress: Option[String] = resource.systemMetadata(Node.NODE_FIELD_PARENT)
          parentAddress match {
            case Some(parentAddress) =>
              this ! RebuildParentTagMsg(Option(parentAddress))
          }
        }

        case RebuildParentTagMsg(resourceAddress) => {
          resourceAddress match {
            case None =>
              log error "got empty resourceAddress"
            case Some(resourceAddress) => {
              try{
                val catalog = accessManager.get(resourceAddress)
                val metadata = catalog.metadata
                val node = Node.fromResource(catalog)

                if (node.isDirectory) {
                  val tags = new mutable.HashMap[String, Int]
                  val dir = node.asInstanceOf[Directory]
                  log info "dir children : " + dir.children.toList

                  dir.children.foreach {  child => {
                    log info "child: " + child
                    val childResource = accessManager.get(child.address)
                    val childTags = childResource.metadata.collectionValue(Resource.MD_TAGS)

                    childTags.foreach(childTag => {
                      if (tags.contains(childTag)) {
                        tags.put(childTag, 1 + tags.get(childTag).get)
                      } else {
                        tags.put(childTag, 1)
                      }}
                    )

                   }
                  }

                  metadata(Resource.MD_TAGS) = MetadataHelper.writeTagMap(tags.toMap[String, Int], (key: String, value: Int) => {key + ":" + value})
                  accessManager.update(catalog)
                }
              } catch {
                case e => e.printStackTrace()
              }
            }
          }
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
                  val tagsBeforeDelete:Map[String, Int] = MetadataHelper.parseTagMap(tagsString, (tagInfo: String) => {
                              (tagInfo.split(":")(0), Int.unbox(tagInfo.split(":")(1)))
                            }).toMap[String, Int]
                  //delete tags
                  val updatedTags = tagsBeforeDelete.map(tagInfo => {
                    if (tags.contains(tagInfo._1)) (tagInfo._1, tagInfo._2 - tags.get(tagInfo._1).get)
                    else tagInfo}).filter(tagInfo => tagInfo._2 > 0)

                  metadata(Resource.MD_TAGS) = MetadataHelper.writeTagMap(updatedTags.toMap[String, Int], (key: String, value: Int) => {key + ":" + value})

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
            case Some(address) =>
              val catalog = accessManager.get(address)
              val metadata = catalog.metadata

              metadata(Resource.MD_TAGS) match {
                case Some(tagsString) =>
                  //collect statistics
                  val tagsBeforeAdd:Map[String, Int] = MetadataHelper.parseTagMap(tagsString, (tagInfo: String) => {
                              (tagInfo.split(":")(0), Int.unbox(tagInfo.split(":")(1)))
                            }).toMap[String, Int]
                  //delete tags
                  val updatedTags = tagsBeforeAdd.map(tagInfo => {
                    if (tags.contains(tagInfo._1)) (tagInfo._1, tagInfo._2 + tags.get(tagInfo._1).get)
                    else tagInfo})

                  metadata(Resource.MD_TAGS) = MetadataHelper.writeTagMap(updatedTags.toMap[String, Int], (key: String, value: Int) => {key + ":" + value})

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
