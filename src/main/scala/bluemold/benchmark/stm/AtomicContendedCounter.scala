package bluemold.benchmark.stm

import bluemold.concurrent.AtomicLong
import java.lang.Thread

/**
 * CasnContendedCounter
 * Author: Neil Essy
 * Created: 9/4/11
 * <p/>
 * [Description]
 */

object AtomicContendedCounter {
  val maxCounter = 100000000L
  val counter = new AtomicLong
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