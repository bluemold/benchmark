package bluemold.benchmark.stm

import scala.concurrent.stm._

object ScalaStmContendedCounter {
  val maxCounter = 10000000L
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
    System.out.println( getClass.getName )
    System.out.println( "Duration: " + duration + "ms" )
    System.out.println( "Transactions/Second: " + ( maxCounter * 1000 / duration ) )
  }
  class Counting extends Runnable {
//    val counter = Ref( 0L )
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        currently = atomic { implicit txn => counter.transformAndGet( _ + 1 ) }
      }
    }
  }
}