package org.aphreet.c3.platform.storage.common

import java.net.InetSocketAddress
import com.sleepycat.je.rep.util.ReplicationGroupAdmin
import com.sleepycat.je._
import java.io.File
import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.storage.{U, StorageIndex, StorageParams}
import org.aphreet.c3.platform.resource.{Resource, IdGenerator}
import rep._
import org.aphreet.c3.platform.exception.{StorageException, ResourceNotFoundException}
import collection.mutable.HashMap
import scala.util.Random

/**
 * Created by IntelliJ IDEA.
 * User: antey
 * Date: 02.05.11
 * Time: 19:57
 * To change this template use File | Settings | File Templates.
 */

abstract class AbstractReplicatedBDBStorage  (override val parameters: StorageParams,
                     override val systemId:String,
                     override val config: BDBConfig) extends AbstractBDBStorage(parameters, systemId, config) {

  protected val NODES_AMOUNT : Int  = 3

  protected val REP_HANDLE_RETRY_MAX : Int = 10

  protected val groupName = storageName.replaceAll("-", "")

  protected var nodesEnvironments = new Array[ReplicatedEnvironment](NODES_AMOUNT)

  protected var databases = new Array[DatabasesPair](NODES_AMOUNT)

  protected var nodesNames = new Array[String](NODES_AMOUNT)

  protected var portsNumbers = new Array[Int](NODES_AMOUNT)

  protected var nodesDirs = new Array[String](NODES_AMOUNT)

  protected var threads = new Array[Thread](NODES_AMOUNT)

  protected var newNodeNumber : Int = 0

  protected var helpers  = new java.util.HashSet[InetSocketAddress]

  protected var rga : ReplicationGroupAdmin = null

  protected var tempRepEnv : ReplicatedEnvironment = null

  protected var envConfig : EnvironmentConfig = null

  protected var masterNodeNumber : Int = 0

  protected val rand = new Random

  {
    open(config)
  }


  def open(bdbConfig : BDBConfig) {
    log info "Opening storage " + id  + " with config " + config

    envConfig = new EnvironmentConfig
    envConfig setAllowCreate true
    envConfig setSharedCache true
    envConfig setTransactional true
    envConfig setCachePercent bdbConfig.cachePercent

    val durability = new Durability(Durability.SyncPolicy.SYNC,
                                    Durability.SyncPolicy.SYNC,
                                    Durability.ReplicaAckPolicy.ALL)

    envConfig setDurability durability

    forAllNodes(i  =>   {
      val tmp = parameters.repParams.get("nodeName-" + i)

      if (tmp != scala.None) {
        nodesNames(i) = tmp.toString
      } else {
        nodesNames(i) = generateNodeName(i)
        parameters.repParams.put("nodeName-" + i, nodesNames(i))
      }
    })

    forAllNodes(i  =>   {
      val tmp = parameters.repParams.get("nodePort-" + i)

      if (tmp != scala.None) {
        try {
          portsNumbers(i) = Integer.parseInt(tmp.toString)
        } catch {
          case e : NumberFormatException => {
            portsNumbers(i) = generatePortNumber(i)
            parameters.repParams.put("nodePort-" + i, portsNumbers(i).toString)
          }
        }

      } else {
        portsNumbers(i) = generatePortNumber(i)
        parameters.repParams.put("nodePort-" + i, portsNumbers(i).toString)
      }
    })

    forAllNodes(i  =>   {
      val tmp = parameters.repParams.get("nodeDir-" + i)

      if (tmp != scala.None) {
        nodesDirs(i) = tmp.toString
      } else {
        nodesDirs(i) = storagePath + "/" + i
        parameters.repParams.put("nodeDir-" + i, nodesDirs(i))
      }
    })

    val tmp = parameters.repParams.get("nodeCounter")
    if (tmp != scala.None) {
      try {
        newNodeNumber = Integer.parseInt( tmp.toString )
      } catch {
        case e : NumberFormatException => {
          newNodeNumber = 0
          parameters.repParams.put("nodeCounter", "0")
        }
      }
    } else {
      newNodeNumber = 0
      parameters.repParams.put("nodeCounter", "0")
    }


    try {
      forAllNodes(i  =>  {
        threads(i) = new Thread( new Runnable() {
            override def run() {
              nodesEnvironments(i) =  createNode( envConfig, groupName, nodesNames(i),
                                                  "localhost:" + portsNumbers(i).toString(),
                                                  "localhost:" + portsNumbers(0).toString(),
                                                  nodesDirs(i))
            }
        })
      })
    } catch {
      case e : IllegalStateException => {
        log.error("Enable to create node config", e)
        //return
      }
    }

    /*for(i <- 0 to NODES_AMOUNT-1) {
      threads(i).start
    }*/
    log info "Starting nodes..."

    forAllNodes(i  =>  {
      threads(i).start
    })

    try {
      forAllNodes(i  =>   {
        threads(i).join

        newNodeNumber += 1
      })
    } catch {
      case e : InterruptedException => log.error("Interrupted exception", e)
    }

    log info "All nodes was started"

    forAllNodes(i  =>   {
      val address : InetSocketAddress = new InetSocketAddress("localhost", portsNumbers(i))
      helpers add address
    })
    rga = new ReplicationGroupAdmin(groupName, helpers)

    masterNodeNumber = getMasterNodeNumber

    log info "Opening database..."

    forAllNodes(i  =>  {
      databases(i) = new DatabasesPair
    })

    val dbConfig : DatabaseConfig = new DatabaseConfig
    dbConfig setAllowCreate true
    dbConfig setTransactional true

    forAllNodes(i  =>   {
      databases(i).database = nodesEnvironments(i).openDatabase(null, storageName, dbConfig)
    })

    log info "Opening secondary databases..."

    for(index <- indexes) {
      log info "Index " + index.name + "..."

      val secConfig = new SecondaryConfig
      secConfig setAllowCreate  true
      secConfig setTransactional  true
      secConfig setSortedDuplicates true
      secConfig.setKeyCreator(new C3SecondaryKeyCreator(index))

      forAllNodes(i  =>   {
        val secDatabase = nodesEnvironments(i).openSecondaryDatabase(null, index.name, databases(i).database, secConfig)

        databases(i).secondaryDatabases.put(index.name, secDatabase)
      })

    }

    log info "Storage " + id + " opened"

    startObjectCounter
  }

  def createIndex(index : StorageIndex) {
    val secConfig = new SecondaryConfig
    secConfig setAllowCreate  true
    secConfig setTransactional  true
    secConfig setSortedDuplicates true
    secConfig.setKeyCreator(new C3SecondaryKeyCreator(index))

    log debug "Creating index: " + index

    forAllNodes(i  =>   {
      val secDatabase = nodesEnvironments(i).openSecondaryDatabase(null, index.name, databases(i).database, secConfig)

      databases(i).secondaryDatabases.put(index.name, secDatabase)
    })

    indexes = index :: indexes

    log debug "Index created: " + index
  }

  def removeIndex(index : StorageIndex) {
    val idxName = index.name

    forAllNodes(i  =>  {
      databases(i).secondaryDatabases.get(idxName) match{
        case None => {}
        case Some(secDb) => {
          secDb.close
          nodesEnvironments(i).removeDatabase(null, idxName)
        }
      }
    })

    indexes = indexes.filter(_.name != index.name)
  }

  override def close = {
    log info "Closing storage " + id
    super.close
    if(this.mode.allowRead)
      mode = U(Constants.STORAGE_MODE_NONE)


    try{

      log info "Closing iterators..."

      val iteratorList = iterators.toList

      for(iterator <- iteratorList){
        iterator.close
      }

    }catch{
      case e => log.error(e)
    }

    log info "Closing secondary databases..."

    forAllNodes(i  =>   {
      for((name, secDb) <- databases(i).secondaryDatabases) {
        secDb.close
      }

      databases(i).secondaryDatabases.clear
    })

    log info "Closing databases..."

    forAllNodes(i  =>   {
      if(databases(i).database != null) {
        databases(i).database.close
        databases(i).database = null
      }
    })

    log info "Closing environments..."

    forAllNodes(i  => {
      if(nodesEnvironments(i) != null) {
        nodesEnvironments(i).cleanLog
        nodesEnvironments(i).close
        nodesEnvironments(i) = null
      }
    })

    databases = null

    nodesEnvironments = null

    log info "Storage " + id + " closed"
  }

  protected def createNode(envConfig : EnvironmentConfig,
                           groupName : String,
                           nodeName : String,
                           nodeHostPort : String,
                           helperHosts : String,
                           fileName : String ) : ReplicatedEnvironment = {
    val repConfig = new ReplicationConfig
    repConfig setGroupName    groupName
    repConfig setNodeName     nodeName
    repConfig setNodeHostPort nodeHostPort
    repConfig setHelperHosts  helperHosts

    val storagePathFile = new File(fileName, "metadata")
    if(!storagePathFile.exists) {
      storagePathFile.mkdirs
    }

    var continueFlag = true

    log info "Creating node " + nodeName + "..."

    for (i <- 0 to REP_HANDLE_RETRY_MAX) {
      if (continueFlag) {
        try {
          return new ReplicatedEnvironment(storagePathFile, repConfig, envConfig)
        } catch {
          case e : UnknownMasterException => {
            e printStackTrace

            Thread.sleep(1000)
          }
          case e : InsufficientLogException =>  {
            //e printStackTrace
            continueFlag = false
          }
        }
      }
    }

    throw new IllegalStateException("Reached max retries!")
  }

  override def getDatabase(writeFlag : Boolean) : Database = {
      var db : Database = null

      if (writeFlag) {
        forAllNodes(i  =>  {
          if (nodesEnvironments(i).isValid && nodesEnvironments(i).getState().isMaster) {
            db = databases(i).database
          }
        })

      } else {
        db = databases( rand.nextInt(3) ).database
      }

      db
  }

  override def getSecondaryDatabases(writeFlag : Boolean) : HashMap[String, SecondaryDatabase] = {
    var dbs : HashMap[String, SecondaryDatabase] = null

    if (writeFlag) {
      forAllNodes(i  =>  {
        if (nodesEnvironments(i).isValid && nodesEnvironments(i).getState().isMaster) {
          dbs = databases(i).secondaryDatabases
        }
      })

    } else {
      dbs = databases( rand.nextInt(3) ).secondaryDatabases
    }

    dbs
  }

  override def getEnvironment() : Environment = {
    var env : ReplicatedEnvironment = null

    forAllNodes(i  =>   {
      if (nodesEnvironments(i).isValid && nodesEnvironments(i).getState().isMaster) {
        env = nodesEnvironments(i)
      }
    })

    env
  }

  def getMasterNodeNumber() : Int = {
    var mNodeNumber : Int = -1

    forAllNodes(i  =>   {
      if (nodesEnvironments(i).isValid && nodesEnvironments(i).getState().isMaster) {
        mNodeNumber = i
      }
    })

    mNodeNumber
  }

  def add(resource:Resource):String = {

    val ra = generateName

    resource.address = ra

    preSave(resource)

    val tx = getEnvironment.beginTransaction(null, null)

    storeData(resource, tx)

    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry(resource.toByteArray)

    var attempts : Int = 0
    var successFlag : Boolean = false

    try {
      do {
        //masterNodeNumber = getMasterNodeNumber

        attempts += 1

        tryToWrite {
          val status = getDatabase(true).putNoOverwrite(tx, key, value)

          if(status != OperationStatus.SUCCESS){
            throw new StorageException("Failed to store resource in database, operation status is: " + status.toString)
          }

          successFlag = true
        }
      } while (!successFlag && attempts <= 2)

      tx.commit

      postSave(resource)

      ra
    } catch {
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def get(ra:String):Option[Resource] = {

    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    if (getDatabase(false).get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      val resource = Resource.fromByteArray(value.getData)
      resource.address = ra
      loadData(resource)
      Some(resource)
    } else None

  }

  def update(resource:Resource):String = {
    val ra = resource.address

    preSave(resource)

    val tx = getEnvironment.beginTransaction(null, null)


    try {

      //Obtaining actual version of resource and locking it for write
      val savedResource:Resource = {
        val key = new DatabaseEntry(ra.getBytes)
        val value = new DatabaseEntry()

        val status = getDatabase(false).get(tx, key, value, LockMode.RMW)
        if(status == OperationStatus.SUCCESS){
          val res = Resource.fromByteArray(value.getData)
          res.address = ra
          res
        }else throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }

      //Replacing metadata
      savedResource.metadata.clear
      savedResource.metadata ++= resource.metadata

      //Appending system metadata
      savedResource.systemMetadata ++= resource.systemMetadata


      for(version <- resource.versions if !version.persisted)
        savedResource.addVersion(version)


      storeData(savedResource, tx)

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry(savedResource.toByteArray)

      var attempts = 0
      var successFlag = false

      do {
        attempts += 1
        //masterNodeNumber = getMasterNodeNumber

        tryToWrite {
          if(getDatabase(true).put(tx, key, value) != OperationStatus.SUCCESS){
            throw new StorageException("Failed to store resource in database")
          } else {
            successFlag = true
          }
        }
      } while (!successFlag && attempts <= 2)

      tx.commit

      postSave(savedResource)

      ra
    }catch{
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def delete(ra:String) = {
    val key = new DatabaseEntry(ra.getBytes)

    val tx = getEnvironment.beginTransaction(null, null)
    try{
      deleteData(ra, tx)

      var attempts = 0
      var successFlag = false
      do {
        attempts += 1
        //masterNodeNumber = getMasterNodeNumber

        tryToWrite {
          val status = getDatabase(true).delete(tx, key)

          if (status != OperationStatus.SUCCESS)
            throw new StorageException("Failed to delete data from DB, op status: " + status.toString)
          else {
            successFlag = true
          }
        }
      } while(!successFlag && attempts <= 2)

      tx.commit
    } catch {
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def put(resource:Resource) = {

    val tx = getEnvironment.beginTransaction(null, null)

    try{
      putData(resource, tx)

      val key = new DatabaseEntry(resource.address.getBytes)
      val value = new DatabaseEntry(resource.toByteArray)

      var attempts = 0
      var successFlag = false

      do {
        attempts += 1
        //masterNodeNumber = getMasterNodeNumber

        tryToWrite {
          val status = getDatabase(true).put(tx, key, value)

          if(status != OperationStatus.SUCCESS){
            throw new StorageException("Failed to store resource in database, operation status: " + status.toString)
          } else {
            successFlag = true
          }
        }
      } while(!successFlag && attempts <= 2)

      tx.commit
    }catch{
      case e=> {
        tx.abort
        throw e
      }
    }
  }

  def appendSystemMetadata(ra:String, metadata:Map[String, String]){

    val tx = getEnvironment.beginTransaction(null, null)

    try{

      //Obtaining actual version of resource and locking it for write
      val savedResource:Resource = {
        val key = new DatabaseEntry(ra.getBytes)
        val value = new DatabaseEntry()

        val status = getDatabase(false).get(tx, key, value, LockMode.RMW)
        if(status == OperationStatus.SUCCESS){
          val res = Resource.fromByteArray(value.getData)
          res.address = ra
          res
        }else throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }
      //Appending system metadata
      savedResource.systemMetadata ++= metadata

      val key = new DatabaseEntry(ra.getBytes)
      val value = new DatabaseEntry(savedResource.toByteArray)

      var attempts = 0
      var successFlag = false

      do {
        attempts += 1
        //masterNodeNumber = getMasterNodeNumber

        tryToWrite {
          if(getDatabase(true).put(tx, key, value) != OperationStatus.SUCCESS){
            throw new StorageException("Failed to store resource in database")
          } else {
            successFlag = true
          }
        }
      } while(!successFlag && attempts <= 2)


      tx.commit
    } catch {
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def lock(ra:String){
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    val tx = getEnvironment.beginTransaction(null, null)

    try{
      val status = getDatabase(false).get(tx, key, value, LockMode.RMW)
      if(status == OperationStatus.SUCCESS){
        val res = Resource.fromByteArray(value.getData)
        res.systemMetadata.get(Resource.SMD_LOCK) match{
          case Some(x) => throw new StorageException("Failed to obtain lock")
          case None =>
        }

        res.systemMetadata.put(Resource.SMD_LOCK, System.currentTimeMillis.toString)

        value.setData(res.toByteArray)

        var attempts = 0
        var successFlag = false
        do {
          tryToWrite {
            attempts += 1
            //masterNodeNumber = getMasterNodeNumber

            getDatabase(true).put(tx, key, value)

            successFlag = true

          }
        } while (!successFlag && attempts <= 2)


        tx.commit
      } else {
        throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }
    } catch {
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def unlock(ra:String){
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    val tx = getEnvironment.beginTransaction(null, null)

    try{
      val status = getDatabase(false).get(tx, key, value, LockMode.RMW)
      if(status == OperationStatus.SUCCESS){
        val res = Resource.fromByteArray(value.getData)


        res.systemMetadata.remove(Resource.SMD_LOCK)

        value.setData(res.toByteArray)

        var attempts = 0
        var successFlag = false
        do {
          tryToWrite {
            attempts += 1
            //masterNodeNumber = getMasterNodeNumber

            getDatabase(true).put(tx, key, value)

            successFlag = true

          }
        } while (!successFlag && attempts <= 2)

        tx.commit
      }else{
        throw new ResourceNotFoundException(
          "Failed to get resource with address " + ra + " Operation status " + status.toString)
      }
    }catch{
      case e => {
        tx.abort
        throw e
      }
    }
  }

  def isAddressExists(address:String):Boolean = {

    val key = new DatabaseEntry(address.getBytes)
    val value = new DatabaseEntry()

    getDatabase(false).get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS
  }

  protected def restartNode(nodeNumberToRestart : Int) {

    log info "Restarting node " + nodeNumberToRestart + "..."

    val newName = generateNodeName(newNodeNumber)
    val newPort = generatePortNumber(newNodeNumber)
    val newDir  = storagePath + "/" + newNodeNumber

    val t = new Thread(new Runnable() {
      override def run() {
              tempRepEnv =  createNode( envConfig, storageName, newName,
                                                  "localhost:" + newPort.toString,
                                                  "localhost:" + generatePortNumber(0).toString(),
                                                  newDir)
      }
    })

    log info "Creating new node \"" + newName + "\"..."

    try {
      t.start
      t.join
      Thread.sleep(5000)
    } catch {
      case e : InterruptedException => e printStackTrace
    }

    val address = new InetSocketAddress("localhost", newPort)
    helpers add address
    rga = new ReplicationGroupAdmin(groupName, helpers)

    try {
      stopNode(nodeNumberToRestart)

      log info "Removing node \"" + nodesNames(nodeNumberToRestart) + "\" from the group \"" + storageName + "\"..."

      rga.removeMember(nodesNames(nodeNumberToRestart))

      log info "Node " + nodesNames(nodeNumberToRestart) + " was successfully removed from the group \"" + storageName + "\'"
    } catch {
      case e : MemberNotFoundException => log.error( "Failed to find the node", e)
      case e : MasterStateException => log.error( "Failed to delete master from the group", e)
    }

    nodesEnvironments(nodeNumberToRestart) = tempRepEnv

    log info "Opening database..."

    val dbConfig : DatabaseConfig = new DatabaseConfig
    dbConfig setAllowCreate true
    dbConfig setTransactional true

    databases(nodeNumberToRestart).database =
      nodesEnvironments(nodeNumberToRestart).openDatabase(null, storageName, dbConfig)

    log info "Opening secondary databases..."

    for(index <- indexes) {
      log info "Index " + index.name + "..."

      val secConfig = new SecondaryConfig
      secConfig setAllowCreate  true
      secConfig setTransactional  true
      secConfig setSortedDuplicates true
      secConfig.setKeyCreator(new C3SecondaryKeyCreator(index))

      val secDatabase =
        nodesEnvironments(nodeNumberToRestart).openSecondaryDatabase(null, index.name, databases(nodeNumberToRestart).database, secConfig)

      databases(nodeNumberToRestart).secondaryDatabases.put(index.name, secDatabase)
    }

    newNodeNumber += 1
    nodesNames(nodeNumberToRestart) = newName
    portsNumbers(nodeNumberToRestart) = newPort
    nodesDirs(nodeNumberToRestart) = newDir

    helpers.clear
    forAllNodes(i  =>   {
      val address : InetSocketAddress = new InetSocketAddress("localhost", portsNumbers(i))
      helpers add address
    })

    parameters.repParams.remove("nodeName-" + nodeNumberToRestart)
    parameters.repParams.remove("nodePort-" + nodeNumberToRestart)
    parameters.repParams.remove("nodeDir-" + nodeNumberToRestart)
    parameters.repParams.remove("nodeCounter")

    parameters.repParams.put("nodeName-" + nodeNumberToRestart, newName)
    parameters.repParams.put("nodePort-" + nodeNumberToRestart, newPort.toString)
    parameters.repParams.put("nodeDir-" + nodeNumberToRestart, newDir)
    parameters.repParams.put("nodeCounter", newNodeNumber.toString)
  }


  protected def generateNodeName(num : Int) : String = {
    val resultName = storageName + "-" + num
    resultName
  }

  protected def generatePortNumber(num : Int) : Int = {
    val portsBase : Int = (62 * (1 + IdGenerator.chars.indexOf( id.charAt(2).toString )) +
                                (1 + IdGenerator.chars.indexOf( id.charAt(3).toString )) )

    val portNum = 30000 + (num + 1) * portsBase
    portNum
  }


  protected def stopNode(num : Int) {
    log info "Node " + nodesNames(num) + " is stopping"

    for((name, secDb) <- databases(num).secondaryDatabases) {
      secDb.close
    }

    log info "Closing secondary database..."

    databases(num).secondaryDatabases.clear

    log info "Closing database..."

    if(databases(num).database != null) {
      databases(num).database.close
      databases(num).database = null
    }

    log info "Closing environment..."

    if(nodesEnvironments(num) != null) {
      nodesEnvironments(num).cleanLog
      nodesEnvironments(num).close
      nodesEnvironments(num) = null
    }

    log info "Node " + nodesNames(num) + " was stopped"
  }


  def restartAliveNodes(deadMasterNodeNumber : Int) {
    forAllNodes(i  =>   {
      if (i != deadMasterNodeNumber) {
        threads(i) = new Thread( new Runnable() {
            override def run() {
              nodesEnvironments(i) =  createNode( envConfig, storageName, nodesNames(i),
                                                  "localhost:" + portsNumbers(i).toString(),
                                                  "localhost:" + portsNumbers(0).toString(),
                                                  storagePath + "-" + i)
            }
        })
      }
    })

    forAllNodes(i  =>   {
      log info "Starting node \"" + nodesNames(i) + "\"..."

      if (i != deadMasterNodeNumber) {
        threads(i).start
      }
    })

    forAllNodes(i  =>   {
      if (i != deadMasterNodeNumber) {
        threads(i).join
      }
    })

    forAllNodes(i  =>  {
      if (i != deadMasterNodeNumber) {
        val dbConfig : DatabaseConfig = new DatabaseConfig
        dbConfig setAllowCreate true
        dbConfig setTransactional true

        databases(i).database = nodesEnvironments(i).openDatabase(null, storageName, dbConfig)


        for(index <- indexes) {

          val secConfig = new SecondaryConfig
          secConfig setAllowCreate  true
          secConfig setTransactional  true
          secConfig setSortedDuplicates true
          secConfig.setKeyCreator(new C3SecondaryKeyCreator(index))

          val secDatabase = nodesEnvironments(i).openSecondaryDatabase(null, index.name,
                                                                           databases(i).database, secConfig)

          databases(i).secondaryDatabases.put(index.name, secDatabase)
        }
      }
    })
  }

  protected def tryToWrite (s: => Unit) = synchronized {
    try{
      s
    } catch {
      case e : InsufficientAcksException => {
        log.error("InsufficientAcksException", e)

        var deadNodeNumber : Int = -1
        forAllNodes(i  =>  {
          if (!nodesEnvironments(i).isValid) {
            deadNodeNumber = i
          }
        })

        if (deadNodeNumber != -1) {
          restartNode(deadNodeNumber)
        }

        masterNodeNumber = getMasterNodeNumber
      }

      case e : LogWriteException => {
        log.error("LogWriteException", e)

        forAllNodes(i  =>  {
          stopNode(i)
        })
        try {
          restartAliveNodes(masterNodeNumber)

          restartNode(masterNodeNumber)

          masterNodeNumber = getMasterNodeNumber

        } catch {
          case ex => {
            throw ex
          }
        }
      }

      case e => {
        throw e
      }
    }
  }

  protected def forAllNodes(f : Int => Unit) {
    for (i <- 0 to NODES_AMOUNT - 1) {
      f(i)
    }
  }

}






