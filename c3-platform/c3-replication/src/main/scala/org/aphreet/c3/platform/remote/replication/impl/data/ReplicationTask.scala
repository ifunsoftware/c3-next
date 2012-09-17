/**
 * Copyright (c) 2010, Mikhail Malygin
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

package org.aphreet.c3.platform.remote.replication.impl.data

import org.aphreet.c3.platform.remote.replication.ReplicationException
import java.io.{DataOutputStream, ByteArrayInputStream, DataInputStream}
import org.apache.commons.io.output.ByteArrayOutputStream

case class ReplicationTask(systemId:String, address:String, action:ReplicationAction) {

  def getKeyBytes:Array[Byte] = {
    val byteOs = new ByteArrayOutputStream
    val dataOs = new DataOutputStream(byteOs)

    val numSystemId = systemId.toInt

    dataOs.writeInt(numSystemId)

    val addressBytes = address.getBytes("UTF-8")
    dataOs.writeInt(addressBytes.size)
    dataOs.write(addressBytes)

    byteOs.toByteArray
  }
}

object ReplicationTask {

  def fromByteArrays(key:Array[Byte], value:Array[Byte]):ReplicationTask = {
    val byteIn = new ByteArrayInputStream(key)
    val dataIn = new DataInputStream(byteIn)

    val systemId = dataIn.readInt
    val addressLength = dataIn.readInt

    val addressBytes = new Array[Byte](addressLength)

    dataIn.read(addressBytes)

    val action = ReplicationAction.fromBytes(value)

    ReplicationTask(systemId.toString, new String(addressBytes, "UTF-8"), action)
  }
}

abstract sealed class ReplicationAction {

  def toBytes:Array[Byte] = {

    val byteOs = new ByteArrayOutputStream
    val dataOs = new DataOutputStream(byteOs)

    this match {
      case UpdateAction(timestamp) => {
        dataOs.writeInt(1)
        dataOs.writeLong(timestamp)
      }
      case AddAction => dataOs.writeInt(2)
      case DeleteAction => dataOs.writeInt(3)
    }

    byteOs.toByteArray
  }

  def isStronger(action:ReplicationAction):Boolean
}

object ReplicationAction {

  def fromBytes(bytes:Array[Byte]):ReplicationAction = {
    val byteIn = new ByteArrayInputStream(bytes)
    val dataIn = new DataInputStream(byteIn)

    val actionCode = dataIn.readInt

    actionCode match {
      case 1 => {
        val timestamp = dataIn.readLong
        UpdateAction(timestamp)
      }
      case 2 => AddAction
      case 3 => DeleteAction
      case _ => throw new ReplicationException("Wrong replication action")
    }
  }
}

case class UpdateAction(timestamp:Long) extends ReplicationAction {

  def isStronger(action:ReplicationAction):Boolean = {
    action match {
      case AddAction => true
      case DeleteAction => false
      case UpdateAction(anotherTimestamp) => timestamp > anotherTimestamp
    }
  }

}
object AddAction extends ReplicationAction {

  def isStronger(action:ReplicationAction):Boolean = false

}
object DeleteAction extends ReplicationAction{

  def isStronger(action:ReplicationAction):Boolean = true
}