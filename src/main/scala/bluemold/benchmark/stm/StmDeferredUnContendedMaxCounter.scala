package bluemold.benchmark.stm

import bluemold.stm._

object StmDeferredUnContendedMaxCounter {
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
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        atomicOn( counter ) {
          deferredUpdate( counter )( _ + 1 )
        } match {
          case Some( value ) => currently = value
          case None => // atomic failed, try again
        }
      }
    }
  }
}