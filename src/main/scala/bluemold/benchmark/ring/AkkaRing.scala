package bluemold.benchmark.ring

import akka.actor._
import akka.actor.Actor._
import java.util.concurrent.CountDownLatch

/**
 * AkkaRing<br/>
 * Author: Neil Essy<br/>
 * Created: 5/19/11<br/>
 * <p/>
 * [Description]
 */

object AkkaRing {
  val numNodes = CommonRingParams.numNodes / 10
  val numMsgs = CommonRingParams.numMsgs
  val latch = new CountDownLatch(numMsgs)
  val createLatch = new CountDownLatch(1)
  val stopLatch = new CountDownLatch(1)
  val actors = new Array[ActorRef]( numMsgs )
  def main(args: Array[String]) {
    val totalMsgs = numNodes * numMsgs - ( numMsgs * numMsgs / 2 )
    println( "***** Benchmark: Ring - Akka (Hawt Dispatcher)" )
    println( "Number of Actors = " + numNodes.formatted( "%,d" ) )
    println( "Number of Messages = " + totalMsgs.formatted( "%,d" ) )

    val rt = Runtime.getRuntime

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedBeforeCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory before creation: " + usedBeforeCreation )

    val system = ActorSystem.create()
    val firstNode = system.actorOf( Props( new AkkaRing(latch,numNodes) ) )
    firstNode ! numNodes
    createLatch.await()

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedAfterCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory after creation: " + usedAfterCreation )
    val memoryPerActor = ( usedAfterCreation - usedBeforeCreation ) / numNodes
    println( "Amoritized memory per actor: " + memoryPerActor )

    synchronized { wait(1000) } // wait one sec before starting

    val startTime = System.currentTimeMillis()

    for ( i <- 0 until numMsgs ) actors(i) ! "hi"

    latch.await()
    val end = System.currentTimeMillis

    val elapsed = end - startTime
    var msgs: Double = numNodes * numMsgs
    msgs /= elapsed
    msgs /= 1000 // this makes it millions per second since elapsed is in ms
    println( "Elapsed = " + elapsed + " ms")
    println( "Millions of messages per second = " + msgs.formatted( "%,.4f" ) )

    firstNode ! "stop"
    stopLatch.await()
    
    println("Stopped")
  }
}

class AkkaRing( latch: CountDownLatch, numActors: Int ) extends Actor {
  import AkkaRing._

  var id: Int = _
  var nextNode: ActorRef = _
  def receive = {
    case count: Int =>
      id = numActors - count
      if ( id < numMsgs ) {
        actors(id) = self
      }
      if ( count > 1 ) {
        nextNode = context.system.actorOf( Props( new AkkaRing(latch,numActors) ) )
        nextNode ! count - 1
      } else {
        createLatch.countDown()
      }

    case "stop" =>
      if (nextNode != null && ! nextNode.isTerminated ) {
        nextNode ! "stop"
      } else {
        stopLatch.countDown()
      }
      self ! PoisonPill

    case "hi" =>
      if (nextNode == null) {
        latch.countDown()
      } else nextNode ! "hi"        
  }
}
