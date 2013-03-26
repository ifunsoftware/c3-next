package org.aphreet.c3.platform.client.management.command

import jline.ConsoleReader
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.management.cli.command.Command
import scala.language.implicitConversions

trait InteractiveCommand extends Command {

  def execute(params:List[String], management:PlatformManagementService, reader:ConsoleReader):String

  def readInput(reader:ConsoleReader):String = reader.readLine

    def readNumber(reader:ConsoleReader):Option[Int] = {

      var correct = false
      var result:Option[Int] = None

      while(!correct){
        try{

          val input = readInput(reader).trim()

          if(input == ""){
            result = None
            correct = true
          }else{
            result = Some(Integer.parseInt(input))
            correct = true
          }
        }catch{
          case e:NumberFormatException => writeString("Incorrect input value, expected number\nTry again: ")
        }
      }
      result
    }

    def readBoolean(reader:ConsoleReader):Option[Boolean] = {
      var correct = false
      var result:Option[Boolean] = None

      while(!correct){
        try{

          result = readInput(reader) match {
            case "true" => Some(true)
            case "false" => Some(false)
            case "" => None
            case _ => throw new Exception("")
          }

          correct = true
        }catch{
          case e:Exception => writeString("Incorrect input value, expected true of false\nTry again: ")
        }
      }
      result
    }

    def writeString(line:String) {
      print(line)
    }

    implicit def convertIntOptionToString(option:Option[Int]):String = {
      option match {
        case Some(value) => value.toString
        case None => ""
      }
    }

    implicit def convertBooleanOptionToString(option:Option[Boolean]):String = {
      option match {
        case Some(value) => value.toString
        case None => ""
      }
    }
}

