package org.aphreet.c3.platform.storage

import java.io.OutputStream

import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource.{Resource, DataWrapper}

import volume.Volume

abstract class Storage {
  
  protected var storageMode:StorageMode = new RW
  
  var volume:Volume = null
  
  var ids:List[String] = List()
  
  def id:String;
  
  def add(resource:Resource):String
  
  def get(ra:String):Option[Resource]

  def update(resource:Resource):String
  
  def delete(ra:String)
  
  def put(resource:Resource)
  
  
  
  def params:StorageParams
  
  def mode:StorageMode = storageMode
  
  def mode_=(newMode:StorageMode) = {
    
    mode.message match {
      case Constants.STORAGE_MODE_MIGRATION => {
        mode match {
          case RW(msg) => {
            if(newMode.allowWrite){
              storageMode = newMode
            }
          }
          case RO(msg) => {
            if((!newMode.allowRead || newMode.allowWrite) 
               && newMode.message != Constants.STORAGE_MODE_USER){
              storageMode = newMode
            }
          }
          
          case U(msg) => {}
        }
      }
      case Constants.STORAGE_MODE_USER => {
        
        mode match {
          case RO(msg) => {
            if(newMode.message == Constants.STORAGE_MODE_USER){
              storageMode = newMode
            }
          }
          
          case U(msg) => {
            if(newMode.message == Constants.STORAGE_MODE_USER){
              storageMode = newMode
            }
          }
          
          case RW(msg) => { 
        	  storageMode = newMode
          }
        }
        
      }
      case Constants.STORAGE_MODE_CAPACITY => {
        newMode.message match{
          case Constants.STORAGE_MODE_CAPACITY => storageMode = newMode
          case Constants.STORAGE_MODE_MIGRATION => {
            if(!newMode.allowWrite){
              storageMode = newMode
            }
          }
        }
        
        
      }
      case Constants.STORAGE_MODE_NONE => {
        if(newMode.message == Constants.STORAGE_MODE_NONE){
          storageMode = newMode
        }else{
          mode match {
            case RW(msg) => storageMode = newMode
            case _ => {}
          }
        }
        
      }
      case _ => {}
    }
    
    if(this.mode != newMode) {
      throw new StorageException("Failed to set mode " + newMode + " due to current mode: " + this.mode)
    }
    
    if(this.mode.allowWrite && this.mode.message == Constants.STORAGE_MODE_USER){
      storageMode = RW(Constants.STORAGE_MODE_NONE)
    }
    
  }
  
  def count:Long
  
  def size:Long
  
  def iterator:StorageIterator
  
  def close;
  
 
  def path:Path
  
  def fullPath:Path
  
  def name:String
}
