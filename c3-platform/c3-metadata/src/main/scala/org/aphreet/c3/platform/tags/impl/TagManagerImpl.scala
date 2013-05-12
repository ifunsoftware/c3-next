package org.aphreet.c3.platform.tags.impl

import javax.annotation.PostConstruct
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.common.msg.RegisterNamedListenerMsg
import org.aphreet.c3.platform.filesystem.{Directory, Node}
import org.aphreet.c3.platform.tags._
import org.aphreet.c3.platform.resource.{MetadataHelper, Resource}
import org.springframework.beans.factory.annotation.{Autowired}
import org.springframework.stereotype.Component
import scala.collection.{mutable, Map}

@Component("TagManager")
class TagManagerImpl extends TagManager with ResourceOwner {

  val log = Logger (getClass)

  @Autowired
  var accessManager: AccessManager = _

  @Autowired
  var accessMediator: AccessMediator = _

  @PostConstruct
  def init() {
    log info "Starting Tag Manager"
    accessMediator ! RegisterNamedListenerMsg(this, 'tagManager)
    this.start()
  }

  override def deleteResource(resource:Resource) {
    resource.metadata(TagManager.TAGS_FIELD).foreach(tagsString => {
      val node:Node = Node.fromResource(resource)

      if (node.isDirectory) {
        val deletedTags:Map[String, Int] = MetadataHelper.parseTagMap(tagsString, (tagInfo: String) => {
          (tagInfo.split(":")(0), tagInfo.split(":")(1).toInt)
        }).toMap[String, Int]

        this ! DeleteParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), deletedTags)
      } else {
        val tagCollection = resource.metadata.collectionValue(TagManager.TAGS_FIELD)

        val tagMap = tagCollection.map(childTag => (childTag , 1)).toMap

        this ! DeleteParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), tagMap)
      }
    })
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
          val tagsString: Option[String] = metadata(TagManager.TAGS_FIELD)
          tagsString match {
            case None => log info "no tags to proccess"
            case Some(tagsS) =>
              log info "ResourceAddedMsg: " + resource.address
              val node:Node = Node.fromResource(resource)

              if (node.isDirectory) {
                val addedTags:Map[String, Int] = MetadataHelper.parseTagMap(tagsS, (tagInfo: String) => {
                  (tagInfo.split(":")(0), tagInfo.split(":")(1).toInt)
                }).toMap[String, Int]
                this ! AddParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), addedTags)
              } else {
                val tagMap = new mutable.HashMap[String, Int]
                val tagCollection = metadata.collectionValue(TagManager.TAGS_FIELD)

                tagCollection.foreach(childTag => {
                  tagMap.put(childTag, 1)})

                this ! AddParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), tagMap)
              }
          }
        }

        case ResourceUpdatedMsg(resource, source) => {
          resource.systemMetadata(Node.NODE_FIELD_PARENT) match {
            case None =>
            case Some(parentAddress) =>
              this ! RebuildParentTagMsg(Option(parentAddress))
          }
        }

        case RebuildParentTagMsg(resourceAddressOption) => {
          resourceAddressOption match {
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
                    val childTags = childResource.metadata.collectionValue(TagManager.TAGS_FIELD)

                    childTags.foreach(childTag => {
                      if (tags.contains(childTag)) {
                        tags.put(childTag, 1 + tags.get(childTag).get)
                      } else {
                        tags.put(childTag, 1)
                      }}
                    )

                  }
                  }

                  metadata(TagManager.TAGS_FIELD) = MetadataHelper.writeTagMap(tags.toMap[String, Int], (key: String, value: Int) => {key + ":" + value})
                  accessManager.update(catalog)
                }
              } catch {
                case e: Throwable => e.printStackTrace()
              }
            }
          }
        }

        case DeleteParentTagMsg(resourceAddressOption, tags) => {
          resourceAddressOption match {
            case None =>
              log error "got empty resourceAddress"
            case Some(resourceAddress) =>
              val catalog = accessManager.get(resourceAddress)
              val metadata = catalog.metadata

              metadata(TagManager.TAGS_FIELD) match {
                case Some(tagsString) =>
                  //collect statistics
                  val tagsBeforeDelete:Map[String, Int] = MetadataHelper.parseTagMap(tagsString, (tagInfo: String) => {
                    (tagInfo.split(":")(0), tagInfo.split(":")(1).toInt)
                  }).toMap[String, Int]
                  //delete tags
                  val updatedTags = tagsBeforeDelete.map(tagInfo => {
                    if (tags.contains(tagInfo._1)) (tagInfo._1, tagInfo._2 - tags.get(tagInfo._1).get)
                    else tagInfo}).filter(tagInfo => tagInfo._2 > 0)

                  metadata(TagManager.TAGS_FIELD) = MetadataHelper.writeTagMap(updatedTags.toMap[String, Int], (key: String, value: Int) => {key + ":" + value})

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
          resourceAddress.foreach (
            address => {
              val catalog = accessManager.get(address)
              val metadata = catalog.metadata

              if (metadata(TagManager.TAGS_FIELD) isEmpty) {
                metadata(TagManager.TAGS_FIELD) = MetadataHelper.writeTagMap(tags.toMap[String, Int], (key: String, value: Int) => {key + ":" + value})
              } else {
                metadata(TagManager.TAGS_FIELD).foreach(
                    tagsString => {
                      //collect statistics
                      val tagsBeforeAdd:Map[String, Int] = MetadataHelper.parseTagMap(tagsString, (tagInfo: String) => {
                        (tagInfo.split(":")(0), tagInfo.split(":")(1).toInt)
                      }).toMap[String, Int]
                      //delete tags
                      val updatedTags = tagsBeforeAdd.map(tagInfo => {
                        if (tags.contains(tagInfo._1)) (tagInfo._1, tagInfo._2 + tags.get(tagInfo._1).get)
                        else tagInfo})

                      metadata(TagManager.TAGS_FIELD) = MetadataHelper.writeTagMap(updatedTags.toMap[String, Int], (key: String, value: Int) => {key + ":" + value})
                    }
                )
              }
              accessManager.update(catalog)
              catalog.systemMetadata(Node.NODE_FIELD_PARENT) foreach {
                address => this ! AddParentTagMsg(Some(address), tags)
              }
            }
          )
        }
      }
    }
  }
}
