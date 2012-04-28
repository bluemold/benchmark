package bluemold.benchmark.ring

import bluemold.actor._
import bluemold.concurrent.{AtomicInteger, AtomicReferenceArray}
import java.util.concurrent.CountDownLatch

/**
 * SimpleRing<br/>
 * Author: Neil Essy<br/>
 * Created: 5/31/11<br/>
 * <p/>
 * [Description]
 */

object StickyOccasionallySlowRing {
  val numNodes = CommonRingParams.numNodesOS
  val numMsgs = CommonRingParams.numMsgsOS
  val firstActors = new AtomicReferenceArray[ActorRef]( numMsgs )
  val creationLatch = new CountDownLatch(1)
  val messagesLatch = new CountDownLatch(numMsgs)
  val stopLatch = new CountDownLatch(1)

  val hiCount = AtomicInteger.create()

  def getHiCount = hiCount.get()

  def main( args: Array[String] ) {
    val strategyFactory = new StickyStrategyFactory()
    implicit val strategy: ActorStrategy = strategyFactory.getStrategy

    println( "***** Benchmark: Occasionally Slow Ring - BlueMold Sticky ( Simple )" )
    println( "Number of Actors = " + numNodes.formatted( "%,d" ) )
    println( "Number of Messages = " + ( numNodes * numMsgs).formatted( "%,d" ) )

    val myActor = new StickyOccasionallySlowRing( strategy ).start()

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
class StickyOccasionallySlowRing( _strategy: ActorStrategy ) extends SimpleActor()( _strategy ) {
  import StickyOccasionallySlowRing._

  var nextActor: ActorRef = null
  var index: Int = 0

  protected def init() {}
  protected def react = null
  override protected def staticBehavior( msg: Any ) {
    msg match {
      case count: Int => {
        if ( count > 0 ) {
          index = numNodes - count
          if ( index < numMsgs )
            firstActors.set( index, self )
          if ( nextActor != null )
            throw new RuntimeException( "if already created the next actor" );
          else nextActor = new StickyOccasionallySlowRing( getNextStrategy() ).start()
          nextActor ! ( count - 1 )
        } else {
          creationLatch.countDown()
        }
      }
      case "hi" => {
        // Occasionally something slow
        if ( index > 0 && ( index % 10000 == 0 ) ) {
          index = 0
          val offLoad = Actor.actorOf( new Actor {
            protected def init() {}
            protected def react = { case msg: Any =>
              var longNoOp = 0
              1 to 10000000 foreach { i => longNoOp += 1 } // takes approx. one tenth of a second
              reply( msg )
              self.stop()
            }
          })( getNextStrategy(), self ).start()
          offLoad ! "hi"
        } else {
          // Normal ring behavior
          hiCount.incrementAndGet()
          if ( nextActor != null )
            nextActor ! "hi"
          else {
            messagesLatch.countDown()
          }
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
}