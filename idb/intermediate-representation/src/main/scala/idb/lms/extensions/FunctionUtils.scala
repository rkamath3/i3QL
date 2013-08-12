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

import scala.virtualization.lms.common.{FunctionsExp, TupledFunctionsExp}

/**
 *
 * @author Ralf Mitschke
 */
trait FunctionUtils
    extends FunctionsExp
    with TupledFunctionsExp
    with ExpressionUtils
{

    def parameterType[A, B] (function: Exp[A => B]): Manifest[Any] = {
        function.tp.typeArguments (0).asInstanceOf[Manifest[Any]]
    }

    def returnType[A, B] (function: Exp[A => B]): Manifest[Any] = {
        function.tp.typeArguments (1).asInstanceOf[Manifest[Any]]
    }

    def parameters[A, B] (function: Exp[A => B]): List[Exp[Any]] = {
        parameter (function) match {
            case UnboxedTuple (xs) => xs
            case x => List (x)
        }
    }

    def parameter[A, B] (function: Exp[A => B]): Exp[A] = {
        function match {
            case Def (Lambda (_, x: Exp[A], _)) => x
            case c@Const (_) => unboxedFresh[A](c.tp.typeArguments (0).typeArguments (0).asInstanceOf[Manifest[A]])
            case _ => throw new IllegalArgumentException ("expected Lambda, found " + function)
        }
    }

    def parameterIndex[A, B] (function: Exp[A => B], x: Exp[Any]): Int = {
        parameters (function).indexOf (x)
    }


    def freeVars[A, B] (function: Exp[A => B]): List[Exp[Any]] = {
        val params = parameters (function).toSet
        val used =
            function match {
                case Def (Lambda (_, _, body)) =>
                    findSyms (body.res)(params).asInstanceOf[Set[Exp[Any]]]
                case Const (_) => Set.empty[Exp[Any]]
                case _ => throw new IllegalArgumentException ("expected Lambda, found " + function)
            }
        params.diff (used).toList
    }


    def body[A, B] (function: Exp[A => B]): Exp[B] = {
        function match {
            case Def (Lambda (_, _, Block (b))) => b
            case c: Const[B@unchecked] => c // a constant function returns a value of type B
            case _ => throw new IllegalArgumentException ("expected Lambda, found " + function)
        }
    }

    def isDisjunctiveParameterEquality[A] (function: Exp[A => Boolean]): Boolean = {
        val params = parameters (function)
        if (params.size != 2) {
            return false
        }
        body (function) match {
            case Def (Equal (lhs, rhs)) => {
                val usedByLeft = findSyms (lhs)(params.toSet)
                val usedByRight = findSyms (rhs)(params.toSet)
                usedByLeft.size == 1 && usedByRight.size == 1 && usedByLeft != usedByRight
            }
            case _ => false
        }
    }


}
