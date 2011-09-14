package bluemold.benchmark.stm

import bluemold.stm._

object StmContendedCounter {
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
        atomic {
// We could cheat when comparing performance by using the deferred* atomic methods which "defer" computation
// until the commit of the transaction. This, however, would not be a fair comparison since this is a unique feature
// of this implementation of STM. The other implementations do not have a corresponding mechanism to compare it to.
//        deferredUpdate( counter )( _ + 1 )
          counter alter ( _ + 1 )
        } match {
          case Some( value ) => currently = value
          case None => // try again
        }
      }
    }
  }
}