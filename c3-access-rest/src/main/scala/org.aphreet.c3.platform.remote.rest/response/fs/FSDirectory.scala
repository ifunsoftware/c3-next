/*
 * Copyright (c) 2011, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.remote.rest.response.fs

import org.aphreet.c3.platform.filesystem.{NodeRef, Directory}
import scala.collection.Map

case class FSDirectory(name:String, address:String, nodes:Array[FSNode])

case class FSNode(name:String, address:String, leaf:Boolean, metadata:Map[String, String]){

  def this(nodeRef:NodeRef, metadata:Map[String, String])
      = this(nodeRef.name, nodeRef.address, nodeRef.leaf, metadata)

}

object FSDirectory{

  def fromNode(node:Directory):FSDirectory = {
    val resource = node.resource

    val name = resource.systemMetadata.get("c3.fs.nodename") match{
      case Some(x) => x
      case None => ""
    }

    val address = resource.address

    FSDirectory(name, address, node.getChildren.map(new FSNode(_, scala.collection.immutable.Map())))
  }

  def fromNodeAndChildren(node:Directory, children:Seq[FSNode]):FSDirectory = {

    val resource = node.resource

    val name = resource.systemMetadata.get("c3.fs.nodename") match{
      case Some(x) => x
      case None => ""
    }

    val address = resource.address

    FSDirectory(name, address, children.toArray)
  }
}