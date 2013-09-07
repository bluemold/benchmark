package bluemold.benchmark.stm

import bluemold.stm._

object StmDeferredUnContendedCounter {
  val maxCounter = 10000000L
  val counterA = Ref( 0L )
  val counterB = Ref( 0L )
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