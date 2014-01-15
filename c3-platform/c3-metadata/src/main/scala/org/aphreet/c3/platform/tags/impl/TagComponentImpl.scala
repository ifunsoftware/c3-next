package org.aphreet.c3.platform.tags.impl

import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.{ComponentLifecycle, Logger}
import org.aphreet.c3.platform.common.msg.{UnregisterNamedListenerMsg, RegisterNamedListenerMsg}
import org.aphreet.c3.platform.filesystem.{Directory, Node}
import org.aphreet.c3.platform.tags._
import org.aphreet.c3.platform.resource.{MetadataHelper, Resource}
import scala.collection.{mutable, Map}
import akka.actor.{PoisonPill, Props, ActorRefFactory, Actor}
import org.aphreet.c3.platform.actor.ActorComponent

trait TagComponentImpl extends TagComponent {

  this: ComponentLifecycle
    with ActorComponent
    with AccessComponent =>

  val tagManager = new TagManagerImpl(actorSystem)

  destroy(Unit => tagManager.destroy())

  class TagManagerImpl(val actorSystem: ActorRefFactory) extends TagManager with ResourceOwner {

    val log = Logger (getClass)

    val async = actorSystem.actorOf(Props.create(classOf[TagManagerActor], this))

    {
      log info "Starting TagManager"
      accessMediator ! RegisterNamedListenerMsg(async, 'tagManager)
    }

    def destroy(){
      log.info("Stopping TagManager")
      accessMediator ! UnregisterNamedListenerMsg(async, 'tagManager)
    }

    override def deleteResource(resource:Resource) {
      resource.metadata(TagManager.TAGS_FIELD).foreach(tagsString => {

        if (isDirectory(resource)) {
          async ! DeleteParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), parseTagsField(tagsString))
        } else {
          val tagCollection = resource.metadata.collectionValue(TagManager.TAGS_FIELD)
          async ! DeleteParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), createIdentityMap(tagCollection))
        }
      })
    }

    private def isDirectory(resource: Resource): Boolean = Node.fromResource(resource).isDirectory

    private def parseTagsField(value: String): Map[String ,Int] = {
      MetadataHelper.parseTagMap(value, (tagInfo: String) => {
        val split = tagInfo.split(":")
        (split(0), split(1).toInt)
      }).toMap[String, Int]
    }

    private def createIdentityMap(tags: TraversableOnce[String]): Map[String, Int] = {
      tags.map(tag => (tag, 1)).toMap[String, Int]
    }

    private def serializeTags(tags: Map[String, Int]): String = {
      MetadataHelper.writeTagMap(tags, (key: String, value: Int) => {key + ":" + value})
    }

    private def mergeTags(existingTags: Map[String, Int], newTags: Map[String, Int], combine: (Int, Int) => Int): Map[String, Int] = {
      existingTags.map(tagInfo => {
        if (newTags.contains(tagInfo._1)) (tagInfo._1, combine(tagInfo._2, newTags.get(tagInfo._1).get))
        else tagInfo})
    }

    class TagManagerActor extends Actor{

      def receive = {
        case ResourceAddedMsg(resource, source) => {
          val metadata = resource.metadata

          metadata(TagManager.TAGS_FIELD) match {
            case None =>
            case Some(tagsS) =>
              log.debug("ResourceAddedMsg: {}", resource.address)

              if (isDirectory(resource)) {
                self ! AddParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), parseTagsField(tagsS))
              } else {
                val tagCollection = metadata.collectionValue(TagManager.TAGS_FIELD)

                self ! AddParentTagMsg(resource.systemMetadata(Node.NODE_FIELD_PARENT), createIdentityMap(tagCollection))
              }
          }
        }

        case ResourceUpdatedMsg(resource, source) => {
          resource.systemMetadata(Node.NODE_FIELD_PARENT)
            .foreach(value => self ! RebuildParentTagMsg(Some(value)))
        }

        case RebuildParentTagMsg(resourceAddressOption) => {
          resourceAddressOption.foreach(resourceAddress => {
            try{
              val catalog = accessManager.get(resourceAddress)
              val metadata = catalog.metadata
              val node = Node.fromResource(catalog)

              if (node.isDirectory) {
                val tags = new mutable.HashMap[String, Int]
                val dir = node.asInstanceOf[Directory]

                if(log.isDebugEnabled){
                  log.debug("dir children : {}", dir.children.toList)
                }

                dir.children.foreach(child => {

                  log.debug("child: {}", child)

                  val childResource = accessManager.get(child.address)
                  val childTags = childResource.metadata.collectionValue(TagManager.TAGS_FIELD)

                  childTags.foreach(childTag =>
                    if (tags.contains(childTag)) {
                      tags.put(childTag, 1 + tags.get(childTag).get)
                    } else {
                      tags.put(childTag, 1)
                    }
                  )
                })

                metadata(TagManager.TAGS_FIELD) = serializeTags(tags)
                accessManager.update(catalog)
              }
            } catch {
              case e: Throwable => log.error("Failed to rebuild parent tag", e)
            }
          })
        }

        case DeleteParentTagMsg(resourceAddressOption, tags) => {
          resourceAddressOption.foreach(resourceAddress => {
            val catalog = accessManager.get(resourceAddress)
            val metadata = catalog.metadata

            metadata(TagManager.TAGS_FIELD) match {
              case Some(tagsString) =>
                //collect statistics
                val tagsBeforeDelete = parseTagsField(tagsString)
                //delete tags
                val updatedTags = mergeTags(tagsBeforeDelete, tags, (a, b) => a - b).filter(_._2 > 0)

                metadata(TagManager.TAGS_FIELD) = serializeTags(updatedTags)

                accessManager.update(catalog)

                catalog.systemMetadata(Node.NODE_FIELD_PARENT) foreach {
                  address => self ! DeleteParentTagMsg(Some(address), tags)
                }

              case None =>
                log info "nothing to delete (" + resourceAddress + ")"
            }
          })
        }

        case AddParentTagMsg(resourceAddress, tags) => {
          resourceAddress.foreach (
            address => {
              val catalog = accessManager.get(address)
              val metadata = catalog.metadata

              if (metadata(TagManager.TAGS_FIELD).isEmpty) {
                metadata(TagManager.TAGS_FIELD) = serializeTags(tags)
              } else {
                metadata(TagManager.TAGS_FIELD).foreach(
                  tagsString => {
                    //collect statistics
                    val tagsBeforeAdd = parseTagsField(tagsString)
                    metadata(TagManager.TAGS_FIELD) = serializeTags(mergeTags(tagsBeforeAdd, tags, (a, b) => a + b))
                  }
                )
              }
              accessManager.update(catalog)
              catalog.systemMetadata(Node.NODE_FIELD_PARENT) foreach {
                address => self ! AddParentTagMsg(Some(address), tags)
              }
            }
          )
        }
      }
    }
  }
}
