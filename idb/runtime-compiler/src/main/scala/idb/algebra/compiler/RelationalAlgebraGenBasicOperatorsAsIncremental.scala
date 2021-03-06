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
 *  Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  Neither the name of the Software Technology Group or Technische
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
package idb.algebra.compiler

import idb.algebra.ir.{RelationalAlgebraIRSetTheoryOperators, RelationalAlgebraIRAggregationOperators, RelationalAlgebraIRRecursiveOperators, RelationalAlgebraIRBasicOperators}
import idb.lms.extensions.{FunctionUtils, CompileScalaExt}
import idb.operators.impl._
import idb.operators.impl.opt._
import scala.virtualization.lms.common._
import idb.MaterializedView
import idb.algebra.print.RelationalAlgebraPrintPlan
import idb.lms.extensions.operations.{SeqOpsExpExt, StringOpsExpExt, OptionOpsExp}

/**
 *
 * @author Ralf Mitschke
 */
trait RelationalAlgebraGenBasicOperatorsAsIncremental
    extends RelationalAlgebraGenBaseAsIncremental
    with CompileScalaExt
    with ScalaGenEffect
{

    val IR: RelationalAlgebraIRBasicOperators
		with RelationalAlgebraIRSetTheoryOperators
		with RelationalAlgebraIRRecursiveOperators
		with RelationalAlgebraIRAggregationOperators
		with RelationalAlgebraSAEBinding
		with FunctionsExp


    import IR._

    // TODO incorporate set semantics into ir
    override def compile[Domain] (query: Rep[Query[Domain]]): Relation[Domain] = {
        query match {
            case Def (Selection (r, f)) => {
                new SelectionView (compile (r), compileFunctionWithDynamicManifests (f), false)
            }
            case Def (Projection (r, f)) => {
                new ProjectionView (compile (r), compileFunctionWithDynamicManifests (f), false)
            }
            case Def (e@CrossProduct (a, b)) => {
				if (e.isIncrementLocal)
					TransactionalCrossProductView (compile(a), compile(b), false).asInstanceOf[Relation[Domain]]
                else
					CrossProductView (compile(a), compile(b), false).asInstanceOf[Relation[Domain]]
            }
            case Def (e@EquiJoin (a, b, eq)) => {
				if (e.isIncrementLocal)
					TransactionalEquiJoinView (compile (a), compile (b), eq.map ((x) => compileFunctionWithDynamicManifests (x._1)),
						eq.map ((x) => compileFunctionWithDynamicManifests (x._2)), false).asInstanceOf[Relation[Domain]]
				else
                	EquiJoinView (compile (a), compile (b), eq.map ((x) => compileFunctionWithDynamicManifests (x._1)),
                    	eq.map ((x) => compileFunctionWithDynamicManifests (x._2)), false).asInstanceOf[Relation[Domain]]
            }

			case Def (e@DuplicateElimination (a)) => {
				if(e.isIncrementLocal)
					new TransactionalDuplicateEliminationView (compile (a), false)
				else
					new DuplicateEliminationView(compile (a), false).asInstanceOf[Relation[Domain]]
			}

			case Def (Unnest (r, f)) => {
				UnNestView(compile (r), compileFunctionWithDynamicManifests(f), false).asInstanceOf[Relation[Domain]]
			}



            case _ => super.compile (query)
        }
    }

}
