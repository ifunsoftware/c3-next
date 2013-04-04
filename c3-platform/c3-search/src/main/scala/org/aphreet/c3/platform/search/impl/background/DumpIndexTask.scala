/*
 * Copyright (c) 2013, Mikhail Malygin
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
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
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

package org.aphreet.c3.platform.search.impl.background

import collection.JavaConversions._
import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter
import java.io.{File, FileOutputStream, BufferedOutputStream}
import javax.xml.stream.XMLOutputFactory
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexReader
import org.apache.lucene.store.NIOFSDirectory
import org.aphreet.c3.platform.common.{CloseableIterator, CloseableIterable, Path}
import org.aphreet.c3.platform.task.IterableTask

class DumpIndexTask(val indexPath: Path, val dumpPath: String) extends IterableTask[Document](new IterableIndex(indexPath)){

  val out = new IndentingXMLStreamWriter(XMLOutputFactory.newInstance()
    .createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream(new File(dumpPath)))))

  override def processElement(document: Document) {
    out.writeStartElement("document")
    for (field <- iterableAsScalaIterable(document.getFields)) {
      out.writeStartElement("field")
      out.writeAttribute("name", field.name)
      out.writeAttribute("value", field.stringValue())
      out.writeEndElement()
    }
    out.writeEndElement()
  }

  override def preStart(){
    out.writeStartDocument()
    out.writeStartElement("documents")
  }

  override def postComplete(){
    out.writeEndElement()
    out.writeEndDocument()
  }

  override def cleanup(){
    out.close()
  }

}

class IterableIndex(val indexPath: Path) extends CloseableIterable[Document]{

  override def iterator = new LuceneIndexIterator(indexPath)

}

class LuceneIndexIterator(val indexPath: Path) extends CloseableIterator[Document] {

  val indexReader = IndexReader.open(new NIOFSDirectory(indexPath.file.getCanonicalFile), true)

  val documentsCount = indexReader.numDocs()

  var currentDocument = 0

  def hasNext = currentDocument < documentsCount

  def next() = {
    try{
      indexReader.document(currentDocument)
    }finally {
      currentDocument = currentDocument + 1
    }
  }

  override def size = documentsCount

  def close() {
    indexReader.close()
  }
}
