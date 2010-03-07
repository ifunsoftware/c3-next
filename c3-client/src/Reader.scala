import org.aphreet.c3.platform.client.access.PlatformReadClient

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 1:32:13 AM
 * To change this template use File | Settings | File Templates.
 */

object Reader{

  def main(args:Array[String]) = new PlatformReadClient(args).run

}