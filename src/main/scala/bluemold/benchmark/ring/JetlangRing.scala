package bluemold.benchmark.ring

import org.jetlang.fibers.PoolFiberFactory
import org.jetlang.channels.MemoryChannel
import java.util.concurrent.{CountDownLatch, TimeUnit, Executors}
import org.jetlang.core.Callback

/**
 * JetlangRing<br/>
 * Author: Neil Essy<br/>
 * Created: 5/21/11<br/>
 * <p/>
 * [Description]
 */

object JetlangRing {
  def main( args: Array[String] ) {
    println( "***** Benchmark: Ring - Jetlang" )

    val numNodes = CommonRingParams.numNodes
    val numMsgs = CommonRingParams.numMsgs
    
    println( "Number of Actors = " + numNodes.formatted( "%,d" ) )
    println( "Number of Messages = " + ( numNodes * numMsgs).formatted( "%,d" ) )

    val pool = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
    val fiberFactory = new PoolFiberFactory(pool)
    val rt = Runtime.getRuntime
    
    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedBeforeCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory before creation: " + usedBeforeCreation )

    val channels = new Array[MemoryChannel[Any]](numNodes)
    val cdl = new CountDownLatch( numMsgs )
    for ( i <- 0 until channels.length ) {
        val fiber = fiberFactory.create()
        val channel = new MemoryChannel[Any]()
        channel.subscribe( fiber, new Callback[Any]() {
          def onMessage( t: Any ) {
            if (i < channels.length-1)
                channels(i+1).publish( t )
            else cdl.countDown()
          }
        } )
        channels(i) = channel
        fiber.start()
    }

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedAfterCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory after creation: " + usedAfterCreation )
    val memoryPerActor = ( usedAfterCreation - usedBeforeCreation ) / numNodes
    println( "Amoritized memory per actor: " + memoryPerActor )

    synchronized { wait(1000) } // wait one sec before starting

    val start = System.currentTimeMillis()

    for( i <- 0 until numMsgs ) {
        channels(i).publish("Hi")
    }
    assert(cdl.await(300,TimeUnit.SECONDS))

    val end = System.currentTimeMillis()

    val elapsed = end - start
    var msgs: Double = numNodes * numMsgs
    msgs /= elapsed
    msgs /= 1000 // this makes it millions per second since elapsed is in ms
    println( "Elapsed = " + elapsed + " ms")
    println( "Millions of messages per second = " + msgs.formatted( "%,.4f" ) )

    pool.shutdown()

    println("Stopped")
  } 
}