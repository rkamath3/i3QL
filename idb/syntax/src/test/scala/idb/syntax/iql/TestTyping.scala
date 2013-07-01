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
package idb.syntax.iql

import org.junit.Test
import org.junit.Assert._
import idb.syntax.iql.impl.{FromClause1, SelectClause1}

import UniversityDatabase._
import idb.syntax.iql.IR._
import idb.schema.university._


/**
 *
 * @author Ralf Mitschke
 */
class TestTyping
{

    @Test
    def testFunctionParametersInFromClause1 () {
        val selectClause = SelectClause1 ((_: Rep[Student]).lastName)

        val fromClause1: IQL_QUERY[Student, String] = FromClause1 (students, selectClause)

        val fun =
            fromClause1 match {
                case FromClause1 (relation, SelectClause1 (project)) => doLambda (project)
            }


        assertEquals (
            scala.collection.immutable.List (
                manifest[Student],
                manifest[String]
            ),
            fun.tp.typeArguments
        )
    }

    @Test
    def testFunctionParametersInProjections () {
        val projectionExp: Rep[Query[String]] = projection (students, (_: Rep[Student]).lastName)


        val fun =
            projectionExp match {
                case Def (Projection (relation, project)) => project
            }

        assertEquals (
            scala.collection.immutable.List (
                manifest[Student],
                manifest[String]
            ),
            fun.tp.typeArguments
        )
    }

    @Test
    def testFunctionParametersInFromClause1Translation () {
        val selectClause = SelectClause1 ((_: Rep[Student]).lastName)

        val fromClause1: IQL_QUERY[Student, String] = FromClause1 (students, selectClause)

        val projectionExp: Rep[Query[String]] =
            fromClause1 match {
                case FromClause1 (relation, SelectClause1 (project)) => projection (relation, project)
            }

        val fun =
            projectionExp match {
                case Def (Projection (relation, project)) => project
            }

        assertEquals (
            scala.collection.immutable.List (
                manifest[Student],
                manifest[String]
            ),
            fun.tp.typeArguments
        )
    }

}
