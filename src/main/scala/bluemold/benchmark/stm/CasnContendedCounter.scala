package bluemold.benchmark.stm

import bluemold.stm._

/**
 * CasnContendedCounter
 * Author: Neil Essy
 * Created: 9/4/11
 * <p/>
 * [Description]
 */

object CasnContendedCounter {
  val maxCounter = 10000000
  val counter = new Ref[Long]( 0L )
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
//    val counter = new Ref[Long]( 0L )
    def run() {
      var currently = 0l
      while ( currently < maxCounter ) {
        atomicOn ( counter ) { _ + 1 } match {
          case Some( value ) => currently = value
          case None => // try again
        }
      }
    }
  }
}