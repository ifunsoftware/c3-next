package org.aphreet.c3.platform.storage.volume.dataprovider

import java.io.File
import org.aphreet.c3.platform.storage.volume.Volume

class WinVolumeDataProvider extends VolumeDataProvider{

  def getVolumeList:List[Volume] =
    for(root <- File.listRoots.toList)
      yield new Volume(root.getAbsolutePath.substring(0, 2).toUpperCase, 
                       root.getTotalSpace, root.getFreeSpace)
}
