package bluemold.benchmark.pingpong

import java.util.concurrent.CountDownLatch
import bluemold.actor.{SimpleActor, ActorStrategy, ActorRef, Actor}


object SimplePingPong {
  val throughput = 10
  val numPairs = Runtime.getRuntime.availableProcessors()
  val numActors = numPairs * 2
  case object Run
  case object Msg

  class Destination( _strategy: ActorStrategy ) extends SimpleActor()( _strategy ) {
    protected def init() {}
    protected def react = null

    override def isTailMessaging = true
    override protected def staticBehavior( msg: Any ) {
      reply( Msg )
    }
    def exposeStrategy = currentStrategy 
  }

  class Client( _strategy: ActorStrategy,actor: ActorRef,
      latch: CountDownLatch,
      repeat: Long ) extends SimpleActor()( _strategy ) {

    val initalMessages = math.min(
      repeat,
      2 * throughput )

    var sent: Long = _
    var received: Long = _

    protected def init() {
      sent = 0L
      received = 0L
    }
    protected def react = null
    override def isTailMessaging = true
    override protected def staticBehavior( msg: Any ) {
      if ( msg == Msg ) {
        received += 1
        if (sent < repeat) {
          sent += 1
          actor ! Msg
        } else if (received >= repeat) {
          latch.countDown()
        }
      } else if ( msg == Run ) {
        for (i <- 0L until initalMessages) {
          sent += 1
          actor ! Msg
        }
      }
    }

  }

  def main( args: Array[String] ) {
    val iterations = 10000000
    val latch = new CountDownLatch(numPairs);
    val dests = Array.fill(numPairs)( new Destination( Actor.defaultStrategy ) )
    val clients = dests map { dest => new Client( dest.exposeStrategy, dest, latch, iterations ).start() }
    dests foreach { _.start }
    val start = System.currentTimeMillis()
    clients foreach { _ ! Run }
    latch.await()
    val end = System.currentTimeMillis()
    val milliseconds = if ( end > start ) end - start else 1
    val seconds = ( milliseconds: Double ) / 1000
    val numMessages = iterations * numActors
    val rate = numMessages / seconds
    val rateInMillions = rate / 1000000 
    println( "Num Pairs: " + numPairs )
    println( "Num Actors: " + numActors )
    println( "Duration: " + milliseconds + "ms" )
    println( "Messages: " + numMessages )
    println( "Million Messages per second: " + rateInMillions )
  }
}