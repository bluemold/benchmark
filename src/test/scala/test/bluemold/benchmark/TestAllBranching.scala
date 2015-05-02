package test.bluemold.benchmark

import junit.framework.{TestSuite, Test, TestCase}
import bluemold.benchmark.branching._

/**
 * TestAllBranching<br/>
 * Author: Neil Essy<br/>
 * Created: 6/6/11<br/>
 * <p/>
 * [Description]
 */

object TestAllBranching {
  def suite: Test = {
      val suite = new TestSuite(classOf[TestAllBranching]);
      suite
  }

  def main(args : Array[String]) {
      junit.textui.TestRunner.run(suite);
  }
}  


class TestAllBranching extends TestCase("branching benchmarks") {

  def testAll() {
    val noArgs = new Array[String](0)
    SimpleBranching.main( noArgs )
    RegisteredBranching.main( noArgs )
    StickySimpleBranching.main( noArgs )
    StickyRegisteredBranching.main( noArgs )
    ExecutorSimpleBranching.main( noArgs )
    ExecutorRegisteredBranching.main( noArgs )
    AkkaBranching.main( noArgs )
  }
}