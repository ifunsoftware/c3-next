package org.aphreet.c3.platform.search.api

/**
 * Created with IntelliJ IDEA.
 * User: jingerbread
 * Date: 7/13/13
 * Time: 1:55 AM
 * To change this template use File | Settings | File Templates.
 */
case class SearchResultElement(address:String,
                                path:String,
                                score:Float,
                                fragments:Array[SearchResultFragment]) {

                                   def setPath(newPath:String):SearchResultElement =
                                     SearchResultElement(address, newPath, score, fragments)

                                 }

case class SearchResultFragment(field:String, foundStrings:Array[String])