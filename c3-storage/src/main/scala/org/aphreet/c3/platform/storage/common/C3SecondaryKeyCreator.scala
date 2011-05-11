package org.aphreet.c3.platform.storage.common

import com.sleepycat.je.{DatabaseEntry, SecondaryDatabase, SecondaryKeyCreator}
import org.aphreet.c3.platform.resource.{Resource, AddressGenerator}
import org.aphreet.c3.platform.storage.StorageIndex

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: Sep 15, 2010
 * Time: 7:11:44 PM
 * To change this template use File | Settings | File Templates.
 */

class C3SecondaryKeyCreator(val index:StorageIndex) extends SecondaryKeyCreator {

  val indexBuilder = new BDBIndexBuilder(index)

  override def createSecondaryKey(database: SecondaryDatabase,
                                  key: DatabaseEntry,
                                  data: DatabaseEntry,
                                  result: DatabaseEntry): Boolean = {

    val address = new String(key.getData)

    if (!AddressGenerator.isValidAddress(address)) return false

    val resource = Resource.fromByteArray(data.getData)

    val byteArray = indexBuilder.createKey(resource)

    if(byteArray != null){
      result.setData(byteArray)
      true
    }else{
      false
    }


  }
}