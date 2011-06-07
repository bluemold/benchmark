package bluemold.benchmark.ring

import java.util.concurrent.{TimeUnit, CountDownLatch}
import scala.actors.scheduler.ForkJoinScheduler
import scala.actors.Reactor
import java.util.concurrent.atomic.AtomicLong

object ScalaRing {
  val msgCount = new AtomicLong
  val rt = Runtime.getRuntime
  val numCores = rt.availableProcessors
  val scheduler = new ForkJoinScheduler(numCores, numCores, false,false)
  scheduler.start()

 def main(args: Array[String]) {
   println( "***** Benchmark: Ring - Scala" )

   val numNodes = 10000
   val numMsgs = 5

   println( "Number of Actors = " + numNodes.formatted( "%,d" ) )
   println( "Number of Messages = " + ( numNodes * numMsgs).formatted( "%,d" ) )

   val rt = Runtime.getRuntime

   synchronized { wait(1000) } // wait one sec before checking memory usage

   rt.gc()
   val usedBeforeCreation = rt.totalMemory() - rt.freeMemory()
   println( "Used memory before creation: " + usedBeforeCreation )

   val channels = new Array[Reactor[Any]](numNodes)
   val cdl = new CountDownLatch( numMsgs )
  var i: Int = 0
  while (i < channels.length) {
    val channel = new ScalaRing(i, channels, cdl)
    channel.start()
    channels(i) = channel
    i += 1
  }

   synchronized { wait(1000) } // wait one sec before checking memory usage

   rt.gc()
   val usedAfterCreation = rt.totalMemory() - rt.freeMemory()
   println( "Used memory after creation: " + usedAfterCreation )
   val memoryPerActor = ( usedAfterCreation - usedBeforeCreation ) / numNodes
   println( "Amoritized memory per actor: " + memoryPerActor )

   synchronized { wait(1000) } // wait one sec before starting

   val start = System.currentTimeMillis()
  i = 0
  while (i < numMsgs) {
    channels(i) ! "Hi"
    i += 1
  }
  cdl.await(1000, TimeUnit.SECONDS)
   val end = System.currentTimeMillis()


   val elapsed = end - start
   var msgs: Double = numNodes * numMsgs
   msgs /= elapsed
   msgs /= 1000 // this makes it millions per second since elapsed is in ms
   println( "Elapsed = " + elapsed + " ms")
   println( "Millions of messages per second = " + msgs.formatted( "%,.4f" ) )

   scheduler.shutdown()

   println( "Stopped" )
 }
}

class ScalaRing(i: Int, channels: Array[Reactor[Any]], cdl: CountDownLatch) extends Reactor[Any] {
  var icount = 0;
  def act() {
    loop {
      react {
        case x:Any =>
          if (i < channels.length -1) {
            icount += 1
            if ( icount % 100 == 0 && i % 500 == 0 )
              println( System.currentTimeMillis() + ": " + i + ": at " + icount )
            channels(i+1) ! x
          }
          else {
            cdl.countDown()
            val count = cdl.getCount
            println( System.currentTimeMillis() + ": " + count )
          }
      }
    }
  }

  override def scheduler = ScalaRing.scheduler
}
