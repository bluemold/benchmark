package bluemold.benchmark.ring

import bluemold.actor._
import bluemold.concurrent.AtomicReferenceArray
import java.util.concurrent.CountDownLatch

/**
 * SimpleRing<br/>
 * Author: Neil Essy<br/>
 * Created: 5/31/11<br/>
 * <p/>
 * [Description]
 */

object StickySimpleRing {
  val numNodes = CommonRingParams.numNodes
  val numMsgs = CommonRingParams.numMsgs
  val firstActors = new AtomicReferenceArray[ActorRef]( numMsgs )
  val creationLatch = new CountDownLatch(1)
  val messagesLatch = new CountDownLatch(numMsgs)
  val stopLatch = new CountDownLatch(1)

  def main( args: Array[String] ) {
    val strategyFactory = new StickyStrategyFactory()
    implicit val strategy: ActorStrategy = strategyFactory.getStrategy

    println( "***** Benchmark: Ring - BlueMold Sticky ( Simple )" )
    println( "Number of Actors = " + numNodes.formatted( "%,d" ) )
    println( "Number of Messages = " + ( numNodes * numMsgs).formatted( "%,d" ) )

    val myActor = new StickySimpleRing( strategy ).start()

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
    val end = System.currentTimeMillis()

    val elapsed = end - start
    var msgs: Double = numNodes * numMsgs
    msgs /= elapsed
    msgs /= 1000 // this makes it millions per second since elapsed is in ms
    println( "Elapsed = " + elapsed + " ms")
    println( "Millions of messages per second = " + msgs.formatted( "%,.4f" ) )

    myActor ! "stop"
    stopLatch.await()

    strategyFactory.printStats()

    strategyFactory.shutdownNow()
    strategyFactory.waitForShutdown()

    println( "Stopped" )
  }
}
class StickySimpleRing( _strategy: ActorStrategy ) extends SimpleActor()( _strategy ) {
  import StickySimpleRing._

  var nextActor: ActorRef = null

  protected def init() {}
  protected def react = null
  override protected def staticBehavior( msg: Any ) {
    msg match {
      case count: Int => {
        if ( count > 0 ) {
          val index = numNodes - count
          if ( index < numMsgs )
            firstActors.set( index, self )
          if ( nextActor != null )
            throw new RuntimeException( "if already created the next actor" );
          else nextActor = new StickySimpleRing( getNextStrategy() ).start()
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
        if ( nextActor != null ) {
          nextActor ! "stop"
          nextActor = null
          self.stop()
        } else {
          nextActor = null
          self.stop()
          stopLatch.countDown()
        }
      }
      case msg: Any => println( msg )
    }
  }
}