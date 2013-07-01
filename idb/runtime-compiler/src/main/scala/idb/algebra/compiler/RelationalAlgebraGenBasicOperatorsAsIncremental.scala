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

import idb.algebra.ir.RelationalAlgebraIRBasicOperators
import idb.lms.extensions.CompileScalaExt
import idb.operators.impl._
import scala.virtualization.lms.common.{FunctionsExp, ScalaGenEffect}
import idb.IndexService

/**
 *
 * @author Ralf Mitschke
 */
trait RelationalAlgebraGenBasicOperatorsAsIncremental
    extends RelationalAlgebraGenBaseAsIncremental
    with CompileScalaExt with ScalaGenEffect
{

    val IR: RelationalAlgebraIRBasicOperators with RelationalAlgebraGenSAEBinding with FunctionsExp

    import IR._

    // TODO incorporate set semantics into ir
    override def compile[Domain: Manifest] (query: Rep[Query[Domain]]): Relation[Domain] = {

	    println("--------->Compile")
        query match {
            case Def (Selection (r, f)) => {
				Predef.println("Selection: " + r.tp)
                new SelectionView (compile (r), compileApplied (f), false)
			}
            case Def (Projection (r, f)) => {
				Predef.println("Projection:")
                Predef.println("    " + r.tp)
                Predef.println("    " + f.tp)
                new ProjectionView (compile (r), compileApplied (f), false)
			}
			case Def (CrossProduct (a, b)) =>  {
				Predef.println("CrossProduct: " + a.tp + " / " + b.tp)
				CrossProductView(compile (a), compile (b), false).asInstanceOf[Relation[Domain]]
			}
			case Def (EquiJoin (a, b, eq)) =>  {
				Predef.println("EquiJoin: " + a.tp + " / " + b.tp)
				EquiJoinView(compile (a), compile (b), eq.map( (x) => compileApplied(x._1) ), eq.map( (x) => compileApplied(x._2) ), false).asInstanceOf[Relation[Domain]]
			}


            case _ => super.compile (query)


        }
}

/*	private def mapSeqA[A : Manifest,B : Manifest](seq : Seq[(Rep[A => Any], Rep[B => Any])]) : Seq[A => Any] = {
		seq.map( (x) => compileApplied(x._1) )
	}

	private def mapSeqB[A : Manifest,B : Manifest](seq : Seq[(Rep[A => Any], Rep[B => Any])]) : Seq[B => Any] = {
		seq.map( (x) => compileApplied(x._2) )
	}  */



}
