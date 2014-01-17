package org.aphreet.c3.platform.remote.replication.impl.data.queue

import java.io.{BufferedWriter, FileWriter, PrintWriter}
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.remote.replication.impl.data.ReplicationTask
import org.aphreet.c3.platform.task.IterableTask

class ReplicationQueueDumpTask(storage: ReplicationQueueStorage, val path: Path) extends IterableTask(storage) {

  val printWriter = new PrintWriter(new BufferedWriter(new FileWriter(path.file)))

  def processElement(element: ReplicationTask) {
    printWriter.print(element.address)
    printWriter.print(element.systemId)
    printWriter.println(element.action)
  }

  override def cleanup() {
    super.postComplete()
    printWriter.close()
  }
}
