package org.aphreet.c3.platform.search.impl

import collection.Map
import java.lang.Float

class SearchConfiguration {

  private var fieldWeights: FieldWeights = new FieldWeights(Map())

  def getFieldWeights: FieldWeights = {
    fieldWeights
  }

  def loadFieldWeight(weights: Map[String, Float]) {
    fieldWeights = new FieldWeights(weights)
  }
}
