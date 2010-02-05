package org.aphreet.c3.platform.client.common

import org.apache.commons.cli._

object ArgumentType extends Enumeration{
    type ArgumentType = Value
    val mandatory, optional = Value
}

abstract class CLI(val args:Array[String]) {
  
  val cli = (new PosixParser).parse(cliDescription, args)
  
  def cliDescription:Options
  
  def cliValue(param:String, default:String):String = {
    if(cli.hasOption(param))
      cli.getOptionValue(param)
    else default
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
  
  class Parameter(var name:String){
  
    private var argName_ = ""
    private var descr_ = ""
    private var optional_ :ArgumentType.Value = ArgumentType.mandatory
  
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
  }  
  
  implicit def convertToParameter(name:String):Parameter = new Parameter(name)
  
}
