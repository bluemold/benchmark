package bluemold.benchmark.branching

import bluemold.actor._
import java.util.concurrent.CountDownLatch

/**
 * SimpleBranching<br/>
 * Author: Neil Essy<br/>
 * Created: 8/10/11<br/>
 * <p/>
 * [Description]
 */



object ExecutorSimpleBranching {
  def getLeaves( acc: Int, level: Int ): Int = if ( level == 0 ) acc else getLeaves( acc * 2, level - 1 )
  val numBounces = 5
  val numLevels = 19
  val numLeaves = getLeaves( 1, numLevels )
  val numActors = numLeaves * 2 - 1
  val numMessages = numActors * numBounces * 2

  val creationLatch = new CountDownLatch(numLeaves)
  val messagesLatch = new CountDownLatch(1)
  val stopLatch = new CountDownLatch(numActors)

  def main( args: Array[String] ) {
    val strategyFactory = new ExecutorStrategyFactory()
    implicit val strategy: ActorStrategy = strategyFactory.getStrategy

    println( "***** Benchmark: Branching - BlueMold Executor ( Simple )" )
    println( "Number of Actors = " + numActors.formatted( "%,d" ) )
    println( "Number of Messages = " + numMessages.formatted( "%,d" ) )

    val rt = Runtime.getRuntime

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedBeforeCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory before creation: " + usedBeforeCreation )

    val myActor = new ExecutorSimpleBranching().start()
    myActor ! (( "create", myActor, numLevels ))
    creationLatch.await()

    
    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedAfterCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory after creation: " + usedAfterCreation )
    val memoryPerActor = ( usedAfterCreation - usedBeforeCreation ) / numActors
    println( "Amoritized memory per actor: " + memoryPerActor )

    synchronized { wait(1000) } // wait one sec before starting

    val startTime = System.currentTimeMillis()
    myActor ! "acc"
    messagesLatch.await()

    val end = System.currentTimeMillis()

    val elapsed = end - startTime
    var msgs: Double = numMessages
    msgs /= elapsed
    msgs /= 1000 // this makes it millions per second since elapsed is in ms
    println( "Elapsed = " + elapsed + " ms")
    println( "Millions of messages per second = " + msgs.formatted( "%,.4f" ) )

    myActor ! "stop"
    stopLatch.await()

    strategyFactory.shutdownNow()
    strategyFactory.waitForShutdown()

    println( "Stopped" )
  }
}
class ExecutorSimpleBranching( implicit _strategy: ActorStrategy ) extends SimpleActor()( _strategy ) {
  import ExecutorSimpleBranching._

  var accBounces: Int = _
  var firstReply: Int = _
  var parent: ActorRef = _ 
  var leftActor: ActorRef = _ 
  var rightActor: ActorRef = _

  protected def init() {
    accBounces = numBounces
  }

  protected def react = null
  override protected def staticBehavior( msg: Any ) {
    msg match {
      case ( "create", parent: ActorRef, count: Int ) => {
        if ( parent != self )
          this.parent = parent
        if ( count > 0 ) {
          if ( leftActor != null )
            throw new RuntimeException( "if already created the left actor" );
          else leftActor = new ExecutorSimpleBranching()(getNextStrategy()).start()
          if ( rightActor != null )
            throw new RuntimeException( "if already created the right actor" );
          else rightActor = new ExecutorSimpleBranching()(getNextStrategy()).start()
          leftActor ! (( "create", self, count - 1 ))
          rightActor ! (( "create", self ,count - 1 ))
        } else {
          creationLatch.countDown()
        }
      }
      case "acc" => {
        if ( leftActor != null && rightActor != null ) {
          firstReply = 0
          leftActor ! "acc"
          rightActor ! "acc"
        } else {
          if ( parent != null )
            parent ! (( "accreply", 1 ))
          else messagesLatch.countDown()
        }
      }
      case ( "accreply", acc: Int ) => {
        if ( firstReply == 0 ) {
          firstReply = acc
        } else {
          if ( parent != null ) {
            parent ! (( "accreply", firstReply + acc ))
          } else {
            println( "acc: " + accBounces + ": " + ( firstReply + acc ) )
            accBounces -= 1
            if ( accBounces == 0 ) {
              messagesLatch.countDown()
            } else self ! "acc"
          }
        }
      }
      case "stop" => {
        if ( leftActor != null )
          leftActor ! "stop"
        if ( rightActor != null )
          rightActor ! "stop"
        stopLatch.countDown()
        parent = null
        leftActor = null
        rightActor = null
        self.stop()
      }
      case msg: Any => println( msg )
    }
  }
}