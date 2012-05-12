package bluemold.benchmark.pingpong

import java.util.concurrent.CountDownLatch


object MethodSequentialPingPong {
  val numProcessors = Runtime.getRuntime.availableProcessors()
  val splicing = 256;
  val numPairs = numProcessors * splicing
  val numActors = numPairs * 2

  case object Msg

  trait Destination {
    def handle[T](msg: T) = Msg
  }

  class Client(actor: Destination,
               latch: CountDownLatch,
               repeat: Long) {

    var sent: Long = _
    var received: Long = _

    def handle() = {
      if (received < repeat)
        received += 1
      if (sent < repeat) {
        sent += 1
        actor.handle(Msg)
      } else if (received == repeat) {
        received += 1
        latch.countDown()
      }
      received <= repeat
    }
  }

  def main(args: Array[String]) {
    val iterations = 800000L
    val latch = new CountDownLatch(numPairs);
    val dests = Array.fill(numPairs)(new Destination {})
    val start = System.currentTimeMillis()
    val clients = dests map {
      dest => new Client(dest, latch, iterations)
    }
    0 until numProcessors foreach {
      i =>
        new Thread("PairGroup-" + i) {
          val group = i

          override def run() {
            0 until splicing foreach {
              j =>
                while (clients(group * splicing + j).handle()) {}
            }
          }
        }.start()
    }
    latch.await()
    val end = System.currentTimeMillis()
    val milliseconds = if (end > start) end - start else 1
    val seconds = (milliseconds: Double) / 1000
    val numMessages = iterations * numActors
    val rate = numMessages / seconds
    val rateInMillions = rate / 1000000
    println("Num Pairs: " + numPairs)
    println("Num Actors: " + numActors)
    println("Duration: " + milliseconds + "ms")
    println("Messages: " + numMessages)
    println("Million Messages per second: " + rateInMillions)
  }
}