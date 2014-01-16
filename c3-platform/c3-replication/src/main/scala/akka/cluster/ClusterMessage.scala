package akka.cluster

/**
 * This class is here just because of bug in akka-osgi, that includes
 * cluster configuration even if we don't have akka-cluster module
 */
trait ClusterMessage {

}
