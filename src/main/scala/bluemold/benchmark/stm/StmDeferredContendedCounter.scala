package bluemold.benchmark.stm

import bluemold.stm._

object StmDeferredContendedCounter {
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