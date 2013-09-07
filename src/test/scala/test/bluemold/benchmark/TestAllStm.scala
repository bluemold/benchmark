package test.bluemold.benchmark

import junit.framework.{TestSuite, Test, TestCase}
import bluemold.benchmark.stm._

object TestAllStm {
  def suite: Test = {
      val suite = new TestSuite(classOf[TestAllStm]);
      suite
  }

  def main(args : Array[String]) {
      junit.textui.TestRunner.run(suite);
  }
}  
class TestAllStm extends TestCase("stm benchmarks") {

  def testAll() {
    val noArgs = new Array[String](0)
/*
    AtomicContendedCounter.main( noArgs )
    AtomicUnContendedCounter.main( noArgs )
    CasnContendedCounter.main( noArgs )
    CasnUnContendedCounter.main( noArgs )
    MultiverseStmContendedCounter.main( noArgs )
    MultiverseStmUnContendedCounter.main( noArgs )
    ScalaStmContendedCounter.main( noArgs )
    ScalaStmUnContendedCounter.main( noArgs )
    StmContendedCounter.main( noArgs )
    StmUnContendedCounter.main( noArgs )
    StmDeferredContendedCounter.main( noArgs )
    StmDeferredUnContendedCounter.main( noArgs )
    TaggedAtomicContendedCounter.main( noArgs )
    TaggedAtomicUnContendedCounter.main( noArgs )
*/

    CasnUnContendedMaxCounter.main( noArgs )
    MultiverseStmUnContendedMaxCounter.main( noArgs )
    ScalaStmUnContendedMaxCounter.main( noArgs )
    StmUnContendedMaxCounter.main( noArgs )
  }
}