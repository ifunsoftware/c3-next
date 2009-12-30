package org.aphreet.c3.platform.common

import java.io.File
import java.lang.Character

/*
 * Abstract path representation
 * Reprepesent path with delimiters "/"
 */

class Path (var path:String){

  val correctPath:String = getCorrectPath(path)
  
  def this(file:File) = this(file.getAbsolutePath)
  
  private def getCorrectPath(path:String):String = {
    
    var newPath:String = path.trim;
    
    if(newPath matches """[A-Za-z]:[\\/].*"""){
      val driveLetter = Character.toUpperCase(path.charAt(0))
      
      newPath = driveLetter + path.substring(1)
    }
    
    
    
    newPath.replaceAll("""\\+""", """/""").replaceFirst("/$","")
  }
  
  override def toString:String = correctPath
  
  def file:File = new File(correctPath)
  
  override def equals(that:Any):Boolean = {
    if(that == null) return false
    
    if(!that.isInstanceOf[Path]) return false
    
    val thatPath = that.asInstanceOf[Path]
    
    if(thatPath.correctPath != this.correctPath) return false
    
    true
  }
  
  override def hashCode:Int = 
    this.correctPath.hashCode
  
  
}
