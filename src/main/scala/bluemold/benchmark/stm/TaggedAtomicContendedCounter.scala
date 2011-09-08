package bluemold.benchmark.stm

import java.lang.Thread
import bluemold.concurrent.AtomicLongNoSpin

/**
 * CasnContendedCounter
 * Author: Neil Essy
 * Created: 9/4/11
 * <p/>
 * [Description]
 */

object TaggedAtomicContendedCounter {
  val maxCounter = 100000000L
  val counter = new AtomicLongNoSpin
  def main( args: Array[String] ) {
    val threadA = new Thread( new Counting )
    val threadB = new Thread( new Counting )
    val start = System.currentTimeMillis()
    threadA.start()
    threadB.start()
    threadA.join()
    threadB.join();
    val end = System.currentTimeMillis()
    val duration = end - start
    System.out.println( "Duration: " + duration + "ms" );
    System.out.println( "Transactions/Second: " + ( maxCounter * 1000 / duration ) );
  }
  class Counting extends Runnable {
//    val counter = new AtomicLong
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        currently = counter.incrementAndGet()
      }
    }
  }
}