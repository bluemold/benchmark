package bluemold.benchmark.stm

import scala.concurrent.stm._

object ScalaStmUnContendedMaxCounter {
  val maxCounter = 10000000L
  def main( args: Array[String] ) {
    val concurrency = Runtime.getRuntime.availableProcessors()
    val threads = Array.fill( concurrency ) { new Thread( new Counting( Ref( 0L ) ) ) }
    val start = System.currentTimeMillis()
    threads foreach { _.start() }
    threads foreach { _.join() }
    val end = System.currentTimeMillis()
    val duration = end - start
    System.out.println( getClass.getName )
    System.out.println( "Duration: " + duration + "ms" )
    System.out.println( "Transactions/Second: " + ( concurrency * maxCounter * 1000 / duration ) )
  }
  class Counting( counter: Ref[Long] ) extends Runnable {
//    val counter = Ref( 0L )
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        currently = atomic { implicit txn => counter.transformAndGet( _ + 1 ) }
      }
    }
  }
}