package bluemold.benchmark.stm

import org.multiverse.api.StmUtils._
import org.multiverse.api.references.TxnLong

object MultiverseStmUnContendedCounter {

  val maxCounter = 10000000L
  val counterA = newTxnLong( 0L )
  val counterB = newTxnLong( 0L )

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
  class Counting( counter: TxnLong ) extends Runnable {
//    val counter = new LongRef( 0L )
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        atomic( new Runnable {
          def run() {
            counter.increment( 1 )
            currently = counter.get()
          }
        })
      }
    }
  }
}