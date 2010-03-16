import java.util.logging.LogManager
import org.aphreet.c3.platform.client.management.ManagementClient

object Shell {

  def main(args:Array[String]) = new ManagementClient(args).run
  
}
