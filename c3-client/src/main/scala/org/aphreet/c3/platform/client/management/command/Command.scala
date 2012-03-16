/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.client.management.command

import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import jline.ConsoleReader

abstract class Command{

  var reader:ConsoleReader = null

  var params:List[String] = List()
  
  var access:PlatformAccessService = null
  
  var management:PlatformManagementService = null
  
  def execute():String
  
  def name:List[String]

  def readInput:String = reader.readLine

  def readNumber:Option[Int] = {

    var correct = false
    var result:Option[Int] = None

    while(!correct){
      try{

        val input = readInput.trim()

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

  def readBoolean:Option[Boolean] = {
    var correct = false
    var result:Option[Boolean] = None

    while(!correct){
      try{

        result = readInput match {
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

  def wrongParameters(usage:String):String = {
    "Not enough parameters. Usage: " +usage
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

abstract class Commands{

  def instances:List[Command]
}