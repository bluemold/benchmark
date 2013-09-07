package bluemold.benchmark.stm

import org.multiverse.api.StmUtils._

object MultiverseStmContendedCounter {

  val maxCounter = 10000000L
  val counter = newTxnLong( 0L )

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