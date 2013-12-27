package org.aphreet.c3.platform.remote.replication.impl

class ReplicationPortRetriever {

  var replicationPort = System.getProperty("replication.port", "7375").toInt

  def setReplicationPort(port:Int) {
    this.replicationPort = port
  }

  def getReplicationPort:Int = {
    replicationPort
  }

}
