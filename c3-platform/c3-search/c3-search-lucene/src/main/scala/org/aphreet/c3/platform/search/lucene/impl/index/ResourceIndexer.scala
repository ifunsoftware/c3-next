package org.aphreet.c3.platform.search.lucene.impl.index

import org.aphreet.c3.platform.resource.Resource
import akka.actor.ActorRef

/**
 * Author: Mikhail Malygin
 * Date:   1/15/14
 * Time:   12:59 AM
 */
trait ResourceIndexer {

  def index(resource: Resource, sender: ActorRef)

  def delete(address: String)

  def updateTextExtractor(textExtractor: TextExtractor)

  def setDocumentExtractionRequired(flag: Boolean)

  def destroy()
}
