import org.aphreet.c3.platform.client.management.connection.ConnectionProvider
import org.aphreet.c3.platform.client.management.connection.impl.{WSConnectionProvider, RmiConnectionProvider}
import org.aphreet.c3.platform.client.management.ManagementClient

object Shell {

  def main(args:Array[String]) = {

    var connectionProvider:ConnectionProvider = null

    if(args.length > 0 && args(0) == "ws")
      connectionProvider = new WSConnectionProvider
    else
      connectionProvider = new RmiConnectionProvider

    new ManagementClient(connectionProvider).run

  }
  
}
