package bluemold.benchmark.stm

import akka.stm._

object AkkaContendedCounter {
  val maxCounter = 1000000L
  val counter = Ref( 0L )
  def main( args: Array[String] ) {
    val threadA = new Thread( new Counting )
    val threadB = new Thread( new Counting )
    val start = System.currentTimeMillis()
    threadA.start()
    threadB.start()
    threadA.join()
    threadB.join()
    val end = System.currentTimeMillis()
    val duration = end - start
    System.out.println( "Duration: " + duration + "ms" )
    System.out.println( "Transactions/Second: " + ( maxCounter * 1000 / duration ) )
  }
  class Counting extends Runnable {
//    val counter = Ref( 0L )
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        currently = atomic { counter alter ( _ + 1 ) }
      }
    }
  }
}