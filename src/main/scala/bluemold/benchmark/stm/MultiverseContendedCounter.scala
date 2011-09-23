package bluemold.benchmark.stm

import org.multiverse.api.GlobalStmInstance._
import org.multiverse.stms.alpha.AlphaStm

object MultiverseContendedCounter {
  val stm = getGlobalStmInstance.asInstanceOf[AlphaStm]
  val refFactory = stm.getProgrammaticRefFactoryBuilder.build()  
  val transFactory = stm.getTransactionFactoryBuilder.build() 

  val maxCounter = 10000000L
  val counter = refFactory.atomicCreateLongRef( 0L )

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
//    val counter = new LongRef( 0L )
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        val t = transFactory.start()
        try {
          counter.inc( 1 )
          currently = counter.get()
        } finally {
          t.commit()
        }
      }
    }
  }
}