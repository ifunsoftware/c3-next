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

package org.aphreet.c3.platform.client.common

import org.apache.commons.cli._
import scala.language.implicitConversions

object ArgumentType extends Enumeration{
    type ArgumentType = Value
    val mandatory, optional = Value
}

abstract class CLI(val args:Array[String]) {

  import ArgumentType._

  val HOST_ARG    : Parameter = "host" has mandatory argument "host" described "Host to connect to" withDefaultVal "http://localhost:7373"
  val USER_ARG    : Parameter = "domain" has mandatory argument "name" described "Domain name" withDefaultVal "anonymous"
  val KEY_ARG     : Parameter = "key"  has mandatory argument "value" described "Secret for specified domain"
  val HELP_ARG    : Parameter = "help" described "Prints this message"
  val OUT_ARG     : Parameter = "out" has mandatory argument "file" described "File to write resource addressed"
  val IN_ARG      : Parameter = "in" has mandatory argument "file" described "File with resource addresses"
  val DIR_ARG     : Parameter = "dir" has mandatory argument "path" described "Directory to store results"
  val FILE_ARG    : Parameter = "file" has mandatory argument "path" described "File or Directory path to upload"
  val THREADS_ARG : Parameter = "threads" has mandatory argument "num" described "Thread count" withDefaultVal "10"
  val SIZE_ARG    : Parameter = "size" has mandatory argument "num" described "Size of object to write" withDefaultVal "512"
  val COUNT_ARG   : Parameter = "count" has mandatory argument "num" described "Count of objects to write" withDefaultVal "10"
  val ID_ARG      : Parameter = "i" has mandatory argument "targetId" described "Id of the target replication system"
  val ADDRESS_ARG : Parameter = "a" has mandatory argument "address" described "Address of the resource"

  val cli = (new PosixParser).parse(cliDescription, args)

  def launch(){
    if(cli.getOptions.length == 0) helpAndExit(clientName)

    if(cli.hasOption(HELP_ARG.name)) helpAndExit(clientName)

    run()
  }

  def run()

  def cliDescription:Options
  
  def cliValue(param:String, default:String):String = {
    if(cli.hasOption(param))
      cli.getOptionValue(param)
    else default
  }

  def cliValue(param:Parameter):String = {
    if(cli.hasOption(param.name)){
      cli.getOptionValue(param.name)
    }else{
      param.default
    }
  }
  
  def helpAndExit(name:String) {
    new HelpFormatter().printHelp(name, cliDescription)
    System.exit(0)
  }
  
  def parameters(params:Parameter*):Options = {
    val options = new Options

    for(param <- params){
      options addOption param.toOption
    }
    options
  }

  def clientName:String
  
  class Parameter(var name:String){
  
    private var argName_ = ""
    private var descr_ = ""
    private var optional_ :ArgumentType.Value = ArgumentType.mandatory
    var default:String = null
  
    def toOption:Option = {
      val option = OptionBuilder.create(name);
      if(argName_.length != 0){
    	option.setArgName(argName_)
    	option.setArgs(1)
      	option.setOptionalArg(optional_ == ArgumentType.optional)
      }
    
      option.setDescription(descr_)
    
      option
    }
  
    def argument(argName:String):Parameter = {
      argName_ = argName
      this
    }

    def described(descr:String):Parameter = {
      descr_ = descr
      this
    }
    
    def has(tp:ArgumentType.Value):Parameter = {
      optional_ = tp
      this
    }

    def withDefaultVal(value:String):Parameter = {
      default = value
      this
    }
  }  
  
  implicit def convertToParameter(name:String):Parameter = new Parameter(name)

  implicit def convertToString(parameter:Parameter):String = cliValue(parameter)

  implicit def convertToInt(parameter:Parameter):Int = cliValue(parameter).toInt
}
