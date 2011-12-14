package bluemold.benchmark.ring

import bluemold.actor._
import bluemold.actor.Actor._
import bluemold.concurrent.AtomicReferenceArray
import java.util.concurrent.CountDownLatch

/**
 * RegisteredRing<br/>
 * Author: Neil Essy<br/>
 * Created: 5/31/11<br/>
 * <p/>
 * [Description]
 */

object RegisteredRing {
  val numNodes = CommonRingParams.numNodes
  val numMsgs = CommonRingParams.numMsgs
  val firstActors = new AtomicReferenceArray[ActorRef]( numMsgs )
  val creationLatch = new CountDownLatch(1)
  val messagesLatch = new CountDownLatch(numMsgs)
  val stopLatch = new CountDownLatch(1)

  def main( args: Array[String] ) {
    println( "***** Benchmark: Ring - BlueMold ( Registered )" )
    println( "Number of Actors = " + numNodes.formatted( "%,d" ) )
    println( "Number of Messages = " + ( numNodes * numMsgs).formatted( "%,d" ) )

    val myActor = actorOf( new RegisteredRing() ).start()

    val rt = Runtime.getRuntime
    
    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedBeforeCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory before creation: " + usedBeforeCreation )

    myActor ! numNodes
      
    creationLatch.await()

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedAfterCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory after creation: " + usedAfterCreation )
    val memoryPerActor = ( usedAfterCreation - usedBeforeCreation ) / numNodes
    println( "Amoritized memory per actor: " + memoryPerActor )

    synchronized { wait(1000) } // wait one sec before starting

    val start = System.currentTimeMillis()
    0 until numMsgs foreach { ( i: Int ) => firstActors.get( i ) ! "hi" }
    messagesLatch.await()
    val end  = System.currentTimeMillis()
    
    val elapsed = end - start
    var msgs: Double = numNodes * numMsgs
    msgs /= elapsed
    msgs /= 1000 // this makes it millions per second since elapsed is in ms
    println( "Elapsed = " + elapsed + " ms")
    println( "Millions of messages per second = " + msgs.formatted( "%,.4f" ) )

    myActor ! "stop"

    stopLatch.await()
    
    println( "Remaining Registered Actors: " + Node.getDefaultNode.getCount )
    println( "Remaining Registered Ids: " + Node.getDefaultNode.getIdCount )
    println( "Remaining Registered ClassNames: " + Node.getDefaultNode.getClassNameCount )
    println( "Remaining Registered Actors by Id: " + Node.getDefaultNode.getIdTotal )
    println( "Remaining Registered Actors by ClassNames: " + Node.getDefaultNode.getClassNameTotal )

    println( "Stopped" )
  }
}
class RegisteredRing extends RegisteredActor {
  import RegisteredRing._

  var nextActor: ActorRef = _

  protected def init() {}

  protected def react = {
      case count: Int => {
        if ( count > 0 ) {
          val index = numNodes - count
          if ( index < numMsgs )
            firstActors.set( index, self )
          if ( nextActor != null )
            throw new RuntimeException( "if already created the next actor" );
          else nextActor = actorOf( new RegisteredRing() ).start()
          nextActor ! ( count - 1 )
        } else {
          creationLatch.countDown()
        }
      }
      case "hi" => {
        if ( nextActor != null )
          nextActor ! "hi"
        else {
          messagesLatch.countDown()
        }
      }
      case "stop" => {
        if ( nextActor != null )
          nextActor ! "stop"
        else {
          stopLatch.countDown()
        }
        nextActor = null
        self.stop()
      }
      case msg: Any => println( msg )
  }
}