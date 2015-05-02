package bluemold.benchmark.ring

import java.util.concurrent.{CountDownLatch, Executors}

import com.lmax.disruptor._
import com.lmax.disruptor.dsl.Disruptor

import scala.annotation.tailrec


object DisruptorRing {
  class MyValue {
    var value: Long = _
  }
  object myFactory extends EventFactory[MyValue] {
    def newInstance() = new MyValue
  }
  val numMessages = 100000
  val latch = new CountDownLatch(numMessages)
  object finalEventHandler extends EventHandler[MyValue] {
    override def onEvent(event: MyValue, sequence: Long, endOfBatch: Boolean): Unit = {
      latch.countDown()
    }
  }
  val translator = new EventTranslatorOneArg[MyValue,Long] {
    override def translateTo(event: MyValue, sequence: Long, value: Long): Unit = {
      event.value = value
    }
  }
  class EventPasser(val nextRingBuffer: RingBuffer[MyValue] ) extends EventHandler[MyValue] {
    override def onEvent(event: MyValue, sequence: Long, endOfBatch: Boolean): Unit = {
      nextRingBuffer.publishEvent(translator,event.value)
    }
  }
  class MyEventProducer(val ringBuffer: RingBuffer[MyValue])
  {
    def onData(value: Long): Unit = ringBuffer.publishEvent(translator,value)
  }

  def main( args: Array[String] ) {
    println( "***** Benchmark: Ring - LMaxDisruptor" )

    val numDisruptors = 1000
    val numNodes = numDisruptors
    val numMsgs = numMessages
    val bufferSize = 1024

    println( "Number of Disruptors = " + numNodes.formatted( "%,d" ) )
    println( "Number of Messages = " + ( numNodes * numMsgs).formatted( "%,d" ) )

    val executor = Executors.newCachedThreadPool()

    val rt = Runtime.getRuntime
    synchronized { wait(1000) } // wait one sec before checking memory usage
    rt.gc()
    val usedBeforeCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory before creation: " + usedBeforeCreation )

    val finalDisruptor = new Disruptor[MyValue](myFactory,bufferSize,executor)
    finalDisruptor.handleEventsWith(finalEventHandler)
    finalDisruptor.start()
    
    @tailrec
    def createDisruptors(disruptors: List[Disruptor[MyValue]], remaining: Int): List[Disruptor[MyValue]] =
      if ( remaining == 0 ) disruptors
      else {
        val nextDisruptor = new Disruptor[MyValue](myFactory,bufferSize,executor)
        nextDisruptor.handleEventsWith(new EventPasser(disruptors.head.getRingBuffer))
        nextDisruptor.start()
        createDisruptors( nextDisruptor :: disruptors, remaining - 1)
      }

    val disruptors = createDisruptors(finalDisruptor :: Nil, numDisruptors - 1)
    val headDisruptor = disruptors.head
    val ring = headDisruptor.getRingBuffer
    val producer = new MyEventProducer(ring)

    synchronized { wait(1000) } // wait one sec before checking memory usage

    rt.gc()
    val usedAfterCreation = rt.totalMemory() - rt.freeMemory()
    println( "Used memory after creation: " + usedAfterCreation )
    val memoryPerActor = ( usedAfterCreation - usedBeforeCreation ) / numNodes
    println( "Amoritized memory per actor: " + memoryPerActor )

    synchronized { wait(1000) } // wait one sec before starting

    val start = System.currentTimeMillis()

    var count = 0L
    while ( count < numMessages ) {
      producer.onData(count)
      count += 1
    }
    latch.await()

    val end = System.currentTimeMillis()

    val elapsed = end - start
    var msgs: Double = numNodes * numMsgs
    msgs /= elapsed
    msgs /= 1000 // this makes it millions per second since elapsed is in ms
    println( "Elapsed = " + elapsed + " ms")
    println( "Millions of messages per second = " + msgs.formatted( "%,.4f" ) )

    disruptors foreach { _.shutdown() }
    executor.shutdown()

    println("Stopped")
  }
}
