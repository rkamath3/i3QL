/* License (BSD Style License):
 *  Copyright (c) 2009, 2011
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package idb.iql.compiler.lms

import org.junit.Test
import org.junit.Assert._
import scala.virtualization.lms.common.{LiftAll, ScalaOpsPkgExp}
import idb.iql.lms.extensions.ScalaOpsExpOptExtensions

/**
 *
 * @author Ralf Mitschke
 *
 */
class TestIROptPushSelection
  extends LiftAll with ScalaOpsExpOptExtensions with ScalaOpsPkgExp with RelationalAlgebraIROptPushSelect
{

  // we require some of our own optimizations (e.g., alpha equivalence) to make the tests work
  assert (this.isInstanceOf[ScalaOpsExpOptExtensions])

  @Test
  def testSelectionOverProjectionSimpleInt ()
  {
    val f1 = (x: Rep[Int]) => x + 2
    val f2 = (x: Rep[Int]) => x > 0

    val expA = selection (projection (baseRelation[Int](), f1), f2)

    val f3 = (x: Rep[Int]) => x > -2

    val expB = projection (selection (baseRelation[Int](), f3), f1)

    assertEquals (expB, expA)
  }

  @Test
  def testSelectionOverProjectionConditionalInt ()
  {
    val f1 = (x: Rep[Int]) => if(x > 0) unit(true) else unit(false)
    val f2 = (x: Rep[Boolean]) => x == true

    val expA = selection (projection (baseRelation[Int](), f1), f2)

    val f3 = (x: Rep[Int]) => x > 0

    val f4 = (x: Rep[Int]) => (if(x > 0) unit(true) else unit(false)) == true

    val expB = projection (selection (baseRelation[Int](), f4), f1)

    assertEquals (expB, expA)
  }

  @Test
  def testSelectionOverProjectionSimpleTuple ()
  {
    val f1 : Rep[Int] => Rep[(Int, Boolean)] = (x: Rep[Int]) => (x, x > 0) // needs annotation for correct implicits
    val f2 = (x: Rep[(Int, Boolean)]) => x._2 == true

    val expA = selection (projection (baseRelation[Int](), f1), f2)

    val f3 = (x: Rep[Int]) => x > 0

    val f4 = (x: Rep[Int]) => (x, x > 0)._2 == true

    val expB = projection (selection (baseRelation[Int](), f4), f1)

    assertEquals (expB, expA)
  }

  @Test
  def testSelectionOverProjectionConditionalTuple ()
  {
    val f1 = (x: Rep[Int]) => if (x > 0) (x, unit (true)) else (x, unit (false))
    val f2 = (x: Rep[(Int, Boolean)]) => x._2 == true

    val expA = selection (projection (baseRelation[Int](), f1), f2)

    val f3 = (x: Rep[Int]) => x > 0

    val f4 = (x: Rep[Int]) => (if (x > 0) (x, unit (true)) else (x, unit (false)))._2 == true

    val expB = projection (selection (baseRelation[Int](), f4), f1)

    assertEquals (expB, expA)
  }

}
