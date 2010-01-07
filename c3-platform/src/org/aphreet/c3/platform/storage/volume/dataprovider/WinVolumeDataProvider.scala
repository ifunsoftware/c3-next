package org.aphreet.c3.platform.storage.volume.dataprovider

import java.io.File

class WinVolumeDataProvider extends VolumeDataProvider{

  def getVolumeList:List[Volume] =
    for(root <- List fromArray File.listRoots)
      yield new Volume(root.getAbsolutePath.substring(0, 2).toUpperCase, 
                       root.getTotalSpace, root.getFreeSpace)
}
