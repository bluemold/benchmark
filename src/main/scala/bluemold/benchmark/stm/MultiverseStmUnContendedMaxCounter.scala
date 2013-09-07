package bluemold.benchmark.stm

import org.multiverse.api.StmUtils._
import org.multiverse.api.references.TxnLong
import java.lang.Runtime

object MultiverseStmUnContendedMaxCounter {

  val maxCounter = 10000000L

  def main( args: Array[String] ) {
    val concurrency = Runtime.getRuntime.availableProcessors()
    val threads = Array.fill( concurrency ) { new Thread( new Counting( newTxnLong( 0L ) ) ) }
    val start = System.currentTimeMillis()
    threads foreach { _.start() }
    threads foreach { _.join() }
    val end = System.currentTimeMillis()
    val duration = end - start
    System.out.println( getClass.getName )
    System.out.println( "Duration: " + duration + "ms" )
    System.out.println( "Transactions/Second: " + ( concurrency * maxCounter * 1000 / duration ) )
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