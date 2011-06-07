package bluemold.benchmark.branching

import scalaz.Scalaz._
import scalaz.concurrent._
import java.util.concurrent.{CountDownLatch, Executors}
import bluemold.concurrent.AtomicInteger

/**
 * ScalazBranching<br/>
 * Author: Neil Essy<br/>
 * Created: 5/30/11<br/>
 * <p/>
 * [Description]
 */

object ScalazBranching {
  def getLeaves( acc: Int, level: Int ): Int = if ( level == 0 ) acc else getLeaves( acc * 2, level - 1 )
  val actorCount = new AtomicInteger()
  val numBounces = 5
  val numLevels = 19
  val numLeaves = getLeaves( 1, numLevels )
  val numActors = numLeaves * 2 - 1
  val numMessages = numActors * numBounces * 2
  val creationLatch = new CountDownLatch(numLeaves)
  val messagesLatch = new CountDownLatch(1)

  implicit val pool = Executors.newFixedThreadPool(5)
  implicit val s = Strategy.Executor

  def main( args: Array[String] ) {
    println( "***** Benchmark: Branching - Scalaz" )
    println( "Number of Actors = " + numActors.formatted( "%,d" ) )
    println( "Number of Messages = " + numMessages.formatted( "%,d" ) )

    val rt = Runtime.getRuntime

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedBeforeCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory before creation: " + usedBeforeCreation )

    val mainActor: Actor[Any] = { val obj = new ScalazBranchingActor( null ); actor { ( msg: Any ) => obj.handler( msg ) } }
    mainActor ! (( "create", mainActor, numLevels ))
    creationLatch.await()

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedAfterCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory after creation: " + usedAfterCreation )
    val memoryPerActor = ( usedAfterCreation - usedBeforeCreation ) / numActors
    println( "Amoritized memory per actor: " + memoryPerActor )

    synchronized { wait(1000) } // wait one sec before starting

    val startTime = System.currentTimeMillis()
    mainActor ! "acc"
    messagesLatch.await()

    val end = System.currentTimeMillis

    val elapsed = end - startTime
    var msgs: Double = numMessages
    msgs /= elapsed
    msgs /= 1000 // this makes it millions per second since elapsed is in ms
    println( "Elapsed = " + elapsed + " ms")
    println( "Millions of messages per second = " + msgs.formatted( "%,.4f" ) )
    
    pool.shutdown()

    println( "Stopped" )
  }
}
class ScalazBranchingActor( parent: Actor[Any] ) {
  import ScalazBranching._  

  actorCount.incrementAndGet()

  protected def init() {
    accBounces = numBounces
  }

  var accBounces: Int = _
  var firstReply: Int = _
  var leftActor: Actor[Any] = _ 
  var rightActor: Actor[Any] = _
  var self: Actor[Any] = _

  def handler( msg: Any ) {
    msg match {
      case ( "create", _self: Actor[Any], count: Int ) => {
        self = _self
        init()
        if ( count > 0 ) {
          if ( leftActor != null )
            throw new RuntimeException( "if already created the left actor" );
          else leftActor = { val obj = new ScalazBranchingActor( self ); actor { ( msg: Any ) => obj.handler( msg ) } }
          if ( rightActor != null )
            throw new RuntimeException( "if already created the right actor" );
          else rightActor = { val obj = new ScalazBranchingActor( self ); actor { ( msg: Any ) => obj.handler( msg ) } }
          leftActor ! (( "create", leftActor, count - 1 ))
          rightActor ! (( "create", rightActor ,count - 1 ))
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
      case msg: Any => println( msg )
    }
  }
}