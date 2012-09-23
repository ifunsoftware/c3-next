package org.aphreet.c3.platform.test.integration.storage

import junit.framework.TestCase
import org.aphreet.c3.platform.storage.volume.dataprovider.VolumeDataProvider


class VolumeDataProviderTestCase extends TestCase{

  def testGatherVolumesData(){

    val provider = VolumeDataProvider.getProvider

    println(provider.getVolumeList)

  }
}
