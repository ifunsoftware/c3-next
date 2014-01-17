package org.aphreet.c3.platform.remote.replication.impl.config

import akka.actor.ActorRefFactory
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import org.aphreet.c3.platform.remote.replication.impl.config.ReplicationNegotiator._
import org.aphreet.c3.platform.remote.replication.impl.data.encryption.{DataEncryptor, AsymmetricDataEncryptor, AsymmetricKeyGenerator}
import org.aphreet.c3.platform.remote.replication.{ReplicationHost, ReplicationException}
import scala.concurrent.Await


/**
 * Author: Mikhail Malygin
 * Date:   1/15/14
 * Time:   4:02 PM
 */
class ReplicationNegotiatorClient(val actorSystem: ActorRefFactory, val localSystemId: String, val configurationManager: ConfigurationManager) {

  implicit val timeout = Timeout(1, TimeUnit.MINUTES)

  def establishReplication(host:String, port:Int, user:String, password:String): ReplicationHost = {

    val keyPair = AsymmetricKeyGenerator.generateKeys

    val name = ReplicationNegotiator.ACTOR_NAME

    val selection = actorSystem.actorSelection(s"akka.tcp://c3-replication@$host:$port/user/$name")

    val negotiator = Await.result(selection.resolveOne(), timeout.duration)

    val keyExchangeFuture = ask(negotiator, NegotiateKeyExchangeMsg(localSystemId, keyPair._1)).mapTo[NegotiateKeyExchangeMsgReply]

    val keyExchangeReply = Await.result(keyExchangeFuture, timeout.duration)

    if(!isOk(keyExchangeReply.status)){
      throw new ReplicationException("Failed to establish replication, status is " + keyExchangeReply.status)
    }

    //base64-encoded key
    val sharedKey = new String(AsymmetricDataEncryptor.decrypt(keyExchangeReply.encryptedSharedKey, keyPair._2), "UTF-8")

    val dataEncryptor = new DataEncryptor(sharedKey)

    val sourceConfiguration = configurationManager.getSerializedConfiguration

    val registerSourceFuture = ask(negotiator, NegotiateRegisterSourceMsg(
      localSystemId,
      dataEncryptor.encrypt(sourceConfiguration),
      dataEncryptor.encrypt(user),
      dataEncryptor.encrypt(password)
    )).mapTo[NegotiateRegisterSourceMsgReply]

    val registerSourceReply = Await.result(registerSourceFuture, timeout.duration)

    if(isOk(registerSourceReply.status)){
      val remoteConfiguration = dataEncryptor.decryptString(registerSourceReply.configuration)
      val platformInfo = configurationManager.deserializeConfiguration(remoteConfiguration)

      val host = platformInfo.host
      host.encryptionKey = sharedKey

      host
    }else{
      throw new ReplicationException("Failed to establish replication")
    }
  }

}
