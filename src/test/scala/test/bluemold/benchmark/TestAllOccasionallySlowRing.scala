package test.bluemold.benchmark

import bluemold.benchmark.ring._
import junit.framework.{TestSuite, Test, TestCase}

/**
 * TestAllRing<br/>
 * Author: Neil Essy<br/>
 * Created: 6/6/11<br/>
 * <p/>
 * [Description]
 */

object TestAllOccasionallySlowRing {
  def suite: Test = {
      val suite = new TestSuite(classOf[TestAllOccasionallySlowRing]);
      suite
  }

  def main(args : Array[String]) {
      junit.textui.TestRunner.run(suite);
  }
}  


class TestAllOccasionallySlowRing extends TestCase("occasionally slow ring benchmarks") {

  def testAll() {
    val noArgs = new Array[String](0)
    OccasionallySlowRing.main( noArgs )
    StickyOccasionallySlowRing.main( noArgs )
    ExecutorOccasionallySlowRing.main( noArgs )
    AkkaOccasionallySlowRing.main( noArgs )
  }
}