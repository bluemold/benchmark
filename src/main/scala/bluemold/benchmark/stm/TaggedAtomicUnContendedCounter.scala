package bluemold.benchmark.stm

import java.lang.Thread
import bluemold.concurrent.AtomicLongNoSpin

object TaggedAtomicUnContendedCounter {
  val maxCounter = 100000000L
  val counterA = new AtomicLongNoSpin
  val counterB = new AtomicLongNoSpin
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
  class Counting( counter: AtomicLongNoSpin ) extends Runnable {
//    val counter = new AtomicLongNoSpin
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        currently = counter.incrementAndGet()
      }
    }
  }
}