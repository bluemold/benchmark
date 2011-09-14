package bluemold.benchmark.ring

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor._
import java.util.concurrent.CountDownLatch

/**
 * AkkaRing<br/>
 * Author: Neil Essy<br/>
 * Created: 5/19/11<br/>
 * <p/>
 * [Description]
 */

object AkkaOccasionallySlowRing {
  val numNodes = 200000
  val numMsgs = 20
  val latch = new CountDownLatch(numMsgs)
  val createLatch = new CountDownLatch(1)
  val stopLatch = new CountDownLatch(1)
  val actors = new Array[ActorRef]( numMsgs )
  def main(args: Array[String]) {
    println( "***** Benchmark: Occasionally Slow Ring - Akka (Hawt Dispatcher)" )
    println( "Number of Actors = " + numNodes.formatted( "%,d" ) )
    println( "Number of Messages = " + ( numNodes * numMsgs).formatted( "%,d" ) )

    val rt = Runtime.getRuntime

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedBeforeCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory before creation: " + usedBeforeCreation )

    val firstNode = actorOf( new AkkaOccasionallySlowRing(latch,numNodes)).start()
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

class AkkaOccasionallySlowRing( latch: CountDownLatch, numActors: Int ) extends Actor {
  import AkkaOccasionallySlowRing._

  var id: Int = _
  var nextNode: ActorRef = _
  var hasBeenSlowYet: Boolean = _

  def receive = {
    case count: Int =>
      id = numActors - count
      if ( id < numMsgs ) {
        actors(id) = self
      }
      if ( count > 1 ) {
        nextNode = actorOf( new AkkaOccasionallySlowRing(latch,numActors)).start()
        nextNode ! count - 1
      } else {
        createLatch.countDown()
      }

    case "stop" =>
      if (nextNode != null && nextNode.isRunning ) {
        nextNode ! "stop"
      } else {
        stopLatch.countDown()
      }
      self.stop()

    case "hi" =>
      // Occasionally something slow
      if ( id > 0 && ( id % 10000 == 0 ) && ! hasBeenSlowYet ) {
        hasBeenSlowYet = true
        val offLoad = actorOf( new Actor {
          def receive = { case msg: Any =>
            var longNoOp = 0
            1 to 10000000 foreach { i => longNoOp += 1 } // takes approx. one tenth of a second
            self.sender match {
              case Some( sender ) => sender ! msg
              case None => throw new RuntimeException( "What Happened?" )
            }
            self.stop()
          }
        }).start()
        offLoad ! "hi"
      } else {
        // Normal ring behavior
        if (nextNode == null) {
          latch.countDown()
        } else nextNode ! "hi"
      }
  }
}
