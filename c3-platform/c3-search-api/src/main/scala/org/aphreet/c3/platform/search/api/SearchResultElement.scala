package org.aphreet.c3.platform.search.api


case class SearchResultElement(address:String,
                                path:String,
                                score:Float,
                                fragments:Array[SearchResultFragment]) {

                                   def setPath(newPath:String):SearchResultElement =
                                     SearchResultElement(address, newPath, score, fragments)

                                 }

case class SearchResultFragment(field:String, foundStrings:Array[String])