import org.aphreet.c3.platform.client.access.PlatformUploadClient

/**
 * Created by IntelliJ IDEA.
 * User: malygm
 * Date: Mar 30, 2010
 * Time: 4:46:40 PM
 * To change this template use File | Settings | File Templates.
 */

object Uploader{

  def main(args:Array[String]) = new PlatformUploadClient(args).run

}