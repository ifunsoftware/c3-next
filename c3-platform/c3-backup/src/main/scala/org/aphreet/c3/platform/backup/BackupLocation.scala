package org.aphreet.c3.platform.backup

import reflect.BeanProperty

case class BackupLocation(
            @BeanProperty var id: String,
            @BeanProperty var backupType: String,
            @BeanProperty var host: String,
            @BeanProperty var user: String,
            @BeanProperty var folder: String,
            @BeanProperty var privateKey: String
        ) extends java.io.Serializable {

  def this() = this(null, null, null, null, null, null)
}
