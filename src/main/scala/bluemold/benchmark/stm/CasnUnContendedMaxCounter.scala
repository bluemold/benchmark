package bluemold.benchmark.stm

import bluemold.concurrent.casn.{CasnVar,CasnSequence}

object CasnUnContendedMaxCounter {
  val maxCounter = 10000000L
  def main( args: Array[String] ) {
    val concurrency = Runtime.getRuntime.availableProcessors()
    val threads = Array.fill( concurrency ) { new Thread( new Counting( new CasnVar( 0L ) ) ) }
    val start = System.currentTimeMillis()
    threads foreach { _.start() }
    threads foreach { _.join() }
    val end = System.currentTimeMillis()
    val duration = end - start
    System.out.println( getClass.getName )
    System.out.println( "Duration: " + duration + "ms" )
    System.out.println( "Transactions/Second: " + ( concurrency * maxCounter * 1000 / duration ) )
  }
  class Counting( counter: CasnVar[Long] ) extends Runnable {
//    val counter = new CasnVar( 0L )
    def run() {
      var currently = 0L
      while ( currently < maxCounter ) {
        CasnSequence
        .update( counter, ( value: Long ) => value + 1 )
        .executeOption() match {
          case Some( value ) => currently = value
          case None => // try again
        }
      }
    }
  }
}