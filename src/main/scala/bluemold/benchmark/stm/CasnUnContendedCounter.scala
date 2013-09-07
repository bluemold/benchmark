package bluemold.benchmark.stm

import bluemold.concurrent.casn.{CasnVar,CasnSequence}

object CasnUnContendedCounter {
  val maxCounter = 10000000L
  val counterA = new CasnVar( 0L )
  val counterB = new CasnVar( 0L )
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