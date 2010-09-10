package org.aphreet.c3.platform.client.common

import org.apache.commons.cli._

object ArgumentType extends Enumeration{
    type ArgumentType = Value
    val mandatory, optional = Value
}

abstract class CLI(val args:Array[String]) {

  import ArgumentType._

  val HOST_ARG : Parameter = "host" has mandatory argument "host" described "Host to connect to" withDefaultVal "http://localhost:8080"
  val USER_ARG : Parameter = "user" has mandatory argument "name" described "User name" withDefaultVal "anonymous"
  val KEY_ARG  : Parameter = "key"  has mandatory argument "value" described "Secret for specified user"
  val HELP_ARG : Parameter = "help" described "Prints this message"
  val OUT_ARG  : Parameter = "out" has mandatory argument "file" described "File to write resource addressed"
  val IN_ARG   : Parameter = "in" has mandatory argument "file" described "File with resource addresses"
  val DIR_ARG  : Parameter = "dir" has mandatory argument "path" described "Directory to store results"
  val FILE_ARG : Parameter = "file" has mandatory argument "path" described "File or Directory path to upload"
  val THREADS_ARG : Parameter = "threads" has mandatory argument "num" described "Thread count" withDefaultVal "10"
  val POOL_ARG : Parameter = "pool" has mandatory argument "name" described "Target pool" withDefaultVal "default"
  val SIZE_ARG : Parameter  = "size" has mandatory argument "num" described "Size of object to write" withDefaultVal "512"
  val COUNT_ARG : Parameter = "count" has mandatory argument "num" described "Count of objects to write" withDefaultVal "10"
  val TYPE_ARG  : Parameter = "type" has mandatory argument "mime" described "Mime type of content" withDefaultVal "application/octet-stream"

  val cli = (new PosixParser).parse(cliDescription, args)

  def launch{
    if(cli.getOptions.length == 0) helpAndExit(clientName)

    if(cli.hasOption(HELP_ARG.name)) helpAndExit(clientName)

    run
  }

  def run

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
  
  def helpAndExit(name:String) = {
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
      optional_ = tp;
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
