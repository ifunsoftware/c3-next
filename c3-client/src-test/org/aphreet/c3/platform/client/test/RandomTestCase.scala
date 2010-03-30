package org.aphreet.c3.platform.client.test

import junit.framework.TestCase
import java.util.Random

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 30, 2010
 * Time: 9:46:15 PM
 * To change this template use File | Settings | File Templates.
 */

class RandomTestCase extends TestCase{

  def testScalaRandom{

    val random = new java.util.Random    

    val array:Array[Int] = Array(0,0,0)

    for(i <- 1 to 10000){
      val randValue = Math.abs(random.nextInt) % 2
      array(randValue) = array(randValue) + 1
    }

    println(array.toString)

  }

}