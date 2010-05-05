package org.aphreet.c3.platform.common

object JSONFormatter {

  def format(text:String):String = {
    var tabCount = 0
    var inQuotes = false
    var prevCharIsEscape = false
    
    val builder = new StringBuilder
    
    for(i <- 0 to text.length - 1){
      val c:String = (text charAt i) + ""
      
      
      
      c match {
        case "{" => {
          builder append c
          if(!inQuotes){
            builder append "\n"
            tabCount = tabCount+1
            nTimesTab(tabCount, builder)
          }
        }
        case "}" => {
          if(!inQuotes){
            builder append "\n"
            tabCount = tabCount-1
            nTimesTab(tabCount, builder)  
          }
          builder append c
        }
        
        case "," => {
          builder append c
          if(!inQuotes){
            builder append "\n"
            nTimesTab(tabCount, builder)
          }
        }
        
        case "\"" => {
          builder append c
          if(!prevCharIsEscape) inQuotes = !inQuotes
        }
        
        case ":" => {
          builder append c
          if(!inQuotes){
            builder append " "
          }
        }
        
        case "[" => {
          builder append c 
          if(!inQuotes){
            builder append "\n"
            tabCount = tabCount + 1
            nTimesTab(tabCount, builder)
          }
        }
        
        case "]" => {
          if(!inQuotes){
            builder append "\n"
            tabCount = tabCount - 1
            nTimesTab(tabCount, builder)
          }
          builder append c
        }

        case "\\" => {
          prevCharIsEscape = !prevCharIsEscape
          builder append c
        }
        
        case _ => builder append c
      }

      if(prevCharIsEscape && c != "\\") prevCharIsEscape = false
      
    }
      
    
    builder.toString
  }
  
  private def nTimesTab(n:Int, builder:StringBuilder) ={
    for(i <- 1 to n)
      builder.append("\t")
  }
}
