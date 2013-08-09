/* License (BSD Style License):
 *  Copyright (c) 2009, 2011
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms,AlphaEquivalence with orAlphaEquivalence without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials providedAlphaEquivalence with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this softwareAlphaEquivalence without specific
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
package idb.lms.extensions.equivalence

import scala.virtualization.lms.common.ScalaOpsPkg

/**
 *
 * @author Ralf Mitschke
 *
 */

trait ScalaOpsPkgExpAlphaEquivalence
    extends ScalaOpsPkg
    with ImplicitOpsExpAlphaEquivalence
    with NumericOpsExpAlphaEquivalence
    with FractionalOpsExpAlphaEquivalence
    with OrderingOpsExpAlphaEquivalence
    with StringOpsExpAlphaEquivalence
    with RangeOpsExpAlphaEquivalence
    with IOOpsExpAlphaEquivalence
    with ArrayOpsExpAlphaEquivalence
    with BooleanOpsExpAlphaEquivalence
    with PrimitiveOpsExpAlphaEquivalence
    with MiscOpsExpAlphaEquivalence
    with FunctionsExpAlphaEquivalence
    with EqualExpAlphaEquivalence
    with IfThenElseExpAlphaEquivalence
    with VariablesExpAlphaEquivalence
    with WhileExpAlphaEquivalence
    with TupleOpsExpAlphaEquivalence
    with ListOpsExpAlphaEquivalence
    with SeqOpsExpAlphaEquivalence
    with DSLOpsExpAlphaEquivalence
    with MathOpsExpAlphaEquivalence
    with CastingOpsExpAlphaEquivalence
    with SetOpsExpAlphaEquivalence
    with ObjectOpsExpAlphaEquivalence
    with ArrayBufferOpsExpAlphaEquivalence
{

}
