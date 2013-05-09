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
package idb.lms.extensions

import scala.virtualization.lms.common.{ScalaOpsPkgExp, LiftAll}
import org.junit.Test

/**
 *
 * @author Ralf Mitschke
 */
class TestAlphaEquivalence
    extends LiftAll with ScalaOpsExpOptExtensions with ScalaOpsPkgExp with LMSTestUtils
{

    @Test
    def testSameMethodBody () {
        assertSameReified (
            (x: Rep[Int]) => x + 1,
            (y: Rep[Int]) => y + 1
        )
    }

    @Test
    def testConstantFolding () {
        assertSameReified (
            (x: Rep[Int]) => x + 1 + 1,
            (y: Rep[Int]) => y + 2
        )
    }

    @Test
    def testMethodConcatenation () {
        def f1 (x: Rep[Int]) = x + 1
        def f2 (x: Rep[Int]) = x + 2

        assertSameReified (
            (x: Rep[Int]) => f1 (f2 (x)),
            (y: Rep[Int]) => y + 3
        )

        assertSameReified (
            (x: Rep[Int]) => f2 (f1 (x)),
            (y: Rep[Int]) => y + 3
        )
    }

    @Test
    def testSandbox () {
        def f1 (t: Rep[Unit]): Rep[Int] = {
            val x: Rep[Int] = 1
            x
        }

        val r1 = fun (f1)

        Predef.println (r1)
    }

}
