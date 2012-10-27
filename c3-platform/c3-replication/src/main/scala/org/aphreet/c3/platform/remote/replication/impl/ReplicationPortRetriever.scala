package org.aphreet.c3.platform.remote.replication.impl

class ReplicationPortRetriever {

  var replicationPort:Int = 0

  def setReplicationPort(port:Int) {
    this.replicationPort = port
  }

  def getReplicationPort:Int = {
    replicationPort
  }

}
