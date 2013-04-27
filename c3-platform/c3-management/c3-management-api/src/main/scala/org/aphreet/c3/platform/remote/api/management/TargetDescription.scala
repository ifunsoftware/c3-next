package org.aphreet.c3.platform.remote.api.management

import reflect.BeanProperty

class TargetDescription(
  @BeanProperty var id: String,
  @BeanProperty var backupType: String,
  @BeanProperty var host: String,
  @BeanProperty var user: String,
  @BeanProperty var folder: String,
  @BeanProperty var privateKey: String)  extends java.io.Serializable {

    def this() = this(null, null, null, null, null, null)
}
