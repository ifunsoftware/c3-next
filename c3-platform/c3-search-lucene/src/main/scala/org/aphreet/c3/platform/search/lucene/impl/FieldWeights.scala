package org.aphreet.c3.platform.search.lucene.impl

import collection.Map
import java.lang.Float

class FieldWeights(val weights: Map[String, Float]) {

  def containsField(key: String): Boolean = {
    weights.contains(key)
  }

  def getBoostFactor(key: String, defaultValue: Float): Float = {
    weights.getOrElse(key, defaultValue)
  }

  def getFields: Array[String] = {
    weights.keys.toArray
  }
}
