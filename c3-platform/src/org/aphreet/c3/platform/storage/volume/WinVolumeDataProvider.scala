package org.aphreet.c3.platform.storage.volume

import java.io.File

class WinVolumeDataProvider extends VolumeDataProvider{

  def getVolumeList:List[Volume] =
    for(root <- List fromArray File.listRoots)
      yield new Volume(root.getAbsolutePath.replace('\\', '/'), 
                       root.getTotalSpace, root.getFreeSpace)
}
