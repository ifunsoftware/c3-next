import org.aphreet.c3.platform.client.management.connection.ConnectionProvider
import org.aphreet.c3.platform.client.management.connection.impl.{WSConnectionProvider, RmiConnectionProvider}
import org.aphreet.c3.platform.client.management.ManagementClient

object Shell {

  def main(args:Array[String]) = {

    var connectionProvider:ConnectionProvider = null

    if(args.length > 1 && args(0) == "ws")
      connectionProvider = new WSConnectionProvider(args(1))
    else
      connectionProvider = new RmiConnectionProvider

    new ManagementClient(connectionProvider).run

  }
  
}
