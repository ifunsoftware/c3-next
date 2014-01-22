package org.aphreet.c3.platform.search.api


case class SearchResultElement(address:String,
                                path:String,
                                score:Float,
                                fragments:Array[SearchResultFragment]) {

                                   def setPath(newPath:String):SearchResultElement =
                                     SearchResultElement(address, newPath, score, fragments)

                                   override def toString = {
                                     "SearchResultElement(address: %s, path: %s score: %f fragments:[ %s ".format( address, path, score, fragments.mkString(","))
                                   }

                                 }

case class SearchResultFragment(field:String, foundStrings:Array[String])  {
  override def toString = {
    "SearchResultFragment(field: %s, foundStrings:[ %s ]".format( field, foundStrings.mkString(","))
  }
}