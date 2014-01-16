package org.aphreet.c3.platform.remote.replication.impl.config

/**
 * Author: Mikhail Malygin
 * Date:   1/15/14
 * Time:   4:18 PM
 */
object ReplicationNegotiator {

  val STATUS_OK = "OK"
  val STATUS_ERROR = "ERROR"

  val ACTOR_NAME = "ReplicationNegotiator"

  def isOk(status: String): Boolean = STATUS_OK == status

  case class NegotiateKeyExchangeMsg(systemId: String, publicKey: Array[Byte]) extends java.io.Serializable

  case class NegotiateKeyExchangeMsgReply(status: String, encryptedSharedKey: Array[Byte]) extends java.io.Serializable

  case class NegotiateRegisterSourceMsg(systemId: String, configuration: Array[Byte], login: Array[Byte], password: Array[Byte]) extends java.io.Serializable

  case class NegotiateRegisterSourceMsgReply(status: String, configuration: Array[Byte]) extends java.io.Serializable

}
