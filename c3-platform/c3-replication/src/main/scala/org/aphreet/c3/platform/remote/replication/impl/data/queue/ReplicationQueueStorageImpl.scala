package org.aphreet.c3.platform.remote.replication.impl.data.queue

import com.sleepycat.je._
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.remote.replication.ReplicationException
import org.aphreet.c3.platform.remote.replication.impl.data.{ReplicationAction, ReplicationTask}

class ReplicationQueueStorageImpl(val path: Path) extends ReplicationQueueStorage {

  val log = LogFactory getLog getClass

  protected var env: Environment = null

  protected var database: Database = null

  {
    open()
  }

  private def open() {
    log info "Opening ReplicationQueueDB..."

    val envConfig = new EnvironmentConfig
    envConfig setAllowCreate true
    envConfig setSharedCache false
    envConfig setTransactional false
    envConfig setDurability Durability.COMMIT_WRITE_NO_SYNC

    val storagePathFile = path.file
    if (!storagePathFile.exists) {
      storagePathFile.mkdirs
    }

    env = new Environment(storagePathFile, envConfig)

    val dbConfig = new DatabaseConfig
    dbConfig setAllowCreate true
    dbConfig setTransactional false
    database = env.openDatabase(null, "ReplicationQueueDB", dbConfig)

    log info "ReplicationQueueDB opened"
  }

  def add(tasks: TraversableOnce[ReplicationTask]) {
    for (task <- tasks) {
      try {
        val key = new DatabaseEntry(task.getKeyBytes)
        val value = new DatabaseEntry()

        val status = database.get(null, key, value, LockMode.DEFAULT)

        status match {
          case OperationStatus.SUCCESS => {
            val queuedAction = ReplicationAction.fromBytes(value.getData)

            if (task.action.isStronger(queuedAction)) {
              value.setData(task.action.toBytes)

              database.put(null, key, value) match {
                case OperationStatus.SUCCESS =>
                case putStatus: OperationStatus =>
                  log warn "Can't put task to queue " + task + " operation status is " + putStatus
              }
            }

          }

          case OperationStatus.NOTFOUND => {
            value.setData(task.action.toBytes)

            database.put(null, key, value) match {
              case OperationStatus.SUCCESS =>
              case putStatus: OperationStatus =>
                log warn "Can't put task to queue " + task + " operation status is " + putStatus
            }
          }
        }
      } catch {
        case e: Throwable => log error("Can't add entry to database", e)
      }
    }
  }

  def iterator: ReplicationQueueIterator = new ReplicationQueueIteratorImpl(database)

  def clear() {

    log.info("Deleting all elements from the replication queue")

    val iterator = this.iterator
    try {
      while (iterator.hasNext) {
        iterator.remove()
      }
      log.info("Replication queue has been cleared")
    } finally {
      iterator.close()
    }
  }

  def close() {

    log info "Closing ReplicationQueueDB..."

    if (database != null) {
      database.close()
      database = null
    }

    if (env != null) {
      env.cleanLog
      env.close()
      env = null
    }

    log info "ReplicationQueueDB closed"
  }

}

class ReplicationQueueIteratorImpl(val database: Database) extends ReplicationQueueIterator{

  val cursor = database.openCursor(null, null)

  private var nextElement = findNextTask()

  override def hasNext: Boolean = {

    nextElement match {
      case Some(task) => true
      case None => false
    }

  }

  override def next(): ReplicationTask = {

    val result = nextElement match {
      case Some(task) => task
      case None => throw new IllegalStateException
    }

    nextElement = findNextTask()

    result
  }

  override def remove() {

    val key = new DatabaseEntry
    val value = new DatabaseEntry

    cursor.getPrev(key, value, LockMode.DEFAULT)

    if (cursor.delete != OperationStatus.SUCCESS) {
      throw new ReplicationException("Failed to remove task from queue")
    }

    nextElement = findNextTask()
  }

  override def size: Int = {
    database.count().toInt
  }

  private def findNextTask(): Option[ReplicationTask] = {

    val key = new DatabaseEntry
    val value = new DatabaseEntry

    if (cursor.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      Some(ReplicationTask.fromByteArrays(key.getData, value.getData))
    } else {
      None
    }
  }

  def close() {
    cursor.close()
  }

}
