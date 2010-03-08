import org.aphreet.c3.platform.client.access.PlatformDeleteClient

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 10:42:02 PM
 * To change this template use File | Settings | File Templates.
 */

object Deleter{

   def main(args:Array[String]) = new PlatformDeleteClient(args).run

}