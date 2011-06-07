package bluemold.benchmark.ring

import scalaz._
import scalaz.Scalaz._
import scalaz.concurrent._
import java.util.concurrent.{CountDownLatch, Executors}

/**
 * ScalazRing<br/>
 * Author: Neil Essy<br/>
 * Created: 5/30/11<br/>
 * <p/>
 * [Description]
 */

object ScalazRing {
  def main( args: Array[String] ) {
    println( "***** Benchmark: Ring - Scalaz" )

    implicit val pool = Executors.newFixedThreadPool(5)
    implicit val s = Strategy.Executor

    val numNodes = 100000
    val numMsgs = 500
    val latch = new CountDownLatch(numMsgs)

    println( "Number of Actors = " + numNodes.formatted( "%,d" ) )
    println( "Number of Messages = " + ( numNodes * numMsgs).formatted( "%,d" ) )

    val rt = Runtime.getRuntime

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedBeforeCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory before creation: " + usedBeforeCreation )

    val actors = new Array[Actor[Int]]( numNodes )
    for ( i <- 0 until numNodes ) { actors(i) = actor { ( index: Int ) =>
      val nextIndex = index + 1
      if ( nextIndex < numNodes )
        actors( nextIndex ) ! nextIndex
      else
        latch.countDown()
    } }

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedAfterCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory after creation: " + usedAfterCreation )
    val memoryPerActor = ( usedAfterCreation - usedBeforeCreation ) / numNodes
    println( "Amoritized memory per actor: " + memoryPerActor )

    synchronized { wait(1000) } // wait one sec before starting

    val start = System.currentTimeMillis()
    0 until numMsgs foreach { ( i: Int ) => actors( i ) ! i } 
    latch.await()

    val end = System.currentTimeMillis()
    val elapsed = end - start
    var msgs: Double = numNodes * numMsgs
    msgs /= elapsed
    msgs /= 1000 // this makes it millions per second since elapsed is in ms
    println( "Elapsed = " + elapsed + " ms")
    println( "Millions of messages per second = " + msgs.formatted( "%,.4f" ) )

    pool.shutdown()

    println( "Stopped" )
  }
}