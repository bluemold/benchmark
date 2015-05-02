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

object TestAllRing {
  def suite: Test = {
      val suite = new TestSuite(classOf[TestAllRing]);
      suite
  }

  def main(args : Array[String]) {
      junit.textui.TestRunner.run(suite);
  }
}  


class TestAllRing extends TestCase("ring benchmarks") {

  def testAll() {
    val noArgs = new Array[String](0)
    JetlangRing.main( noArgs )
    SimpleRing.main( noArgs )
    RegisteredRing.main( noArgs )
    StickySimpleRing.main( noArgs )
    StickyRegisteredRing.main( noArgs )
    ExecutorSimpleRing.main( noArgs )
    ExecutorRegisteredRing.main( noArgs )
    AkkaRing.main( noArgs )
  }
}