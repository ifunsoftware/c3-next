package org.aphreet.c3.platform.client.management.command.impl

import org.aphreet.c3.platform.remote.api.management.Pair
import org.aphreet.c3.platform.client.management.command.Command
import collection.jcl.{SortedSet}
import collection.immutable.TreeSet

class GetPlatformPropertiesCommand extends Command{

  def execute:String = {
    //val map:Array[Pair] = management.platformProperties


    val set = new TreeSet[Pair]

    (set ++ management.platformProperties).map(e => e.key + "=" + e.value + "\n").foldLeft("")(_ + _)

  }
  
  def name:List[String] = List("list", "platform", "properties")

  implicit def toOrdered(pair:Pair):Ordered[Pair] = {
    new OrderedPair(pair)
  }

  class OrderedPair(val pair:Pair) extends Ordered[Pair] {

    override def compare(that:Pair):Int = {
      that.key.compareTo(pair.key)
    }
  }
}
