package bluemold.benchmark.stm

import bluemold.concurrent.AtomicLong
import java.lang.Thread

object AtomicUnContendedCounter {
  val maxCounter = 100000000L
  val counterA = new AtomicLong
  val counterB = new AtomicLong
  def main( args: Array[String] ) {
    val threadA = new Thread( new Counting( counterA ) )
    val threadB = new Thread( new Counting( counterB ) )
    val start = System.currentTimeMillis()
    threadA.start()
    threadB.start()
    threadA.join()
    threadB.join()
    val end = System.currentTimeMillis()
    val duration = end - start
    System.out.println( getClass.getName )
    System.out.println( "Duration: " + duration + "ms" )
    System.out.println( "Transactions/Second: " + ( 2 * maxCounter * 1000 / duration ) )
  }
  class Counting( counter: AtomicLong ) extends Runnable {
//    val counter = new AtomicLong
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        currently = counter.incrementAndGet()
      }
    }
  }
}