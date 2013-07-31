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

import UniversityDatabase._
import TestUtil.assertEqualStructure
import idb.schema.university._
import idb.syntax.iql.IR._
import org.junit.{Ignore, Test}

/**
 * Test clauses with three relations.
 * By convention the algebra representation will currently contain binary nodes, where the the first two relations
 * are always constructed as leaves of one node and tail relations are added as single leaves of parent nodes.
 *
 * @author Ralf Mitschke
 */
class TestBasicClauses3
{

    @Test
    def testCrossProduct3 () {
        val query = plan (
            SELECT (*) FROM(students, registrations, courses)
        )

        assertEqualStructure (
            projection (
                crossProduct (
                    crossProduct (extent (students), extent (registrations)),
                    extent (courses)
                ),
                fun (
                    (sr_c: Rep[((Student, Registration), Course)]) => (sr_c._1._1, sr_c._1._2, sr_c._2)
                )
            ),
            query
        )
    }


    @Test
    def testCrossProduct3Selection1st () {
        val query = plan (
            SELECT (*) FROM(students, registrations, courses) WHERE (
                (s: Rep[Student], r: Rep[Registration], c: Rep[Course]) => {
                    s.firstName == "Sally"
                }
                )
        )

        assertEqualStructure (
            projection (
                crossProduct (
                    crossProduct (
                        selection (extent (students), (s: Rep[Student]) => s.firstName == "Sally"),
                        extent (registrations)
                    ),
                    extent (courses)
                ),
                fun (
                    (sr_c: Rep[((Student, Registration), Course)]) => (sr_c._1._1, sr_c._1._2, sr_c._2)
                )
            ),
            query
        )
    }


    @Test
    def testCrossProduct3Selection1stAnd2nd () {
        val query = plan (
            SELECT (*) FROM(students, registrations, courses) WHERE (
                (s: Rep[Student], r: Rep[Registration], c: Rep[Course]) => {
                    s.firstName == "Sally" &&
                        r.comment == "This is an introductory Course"
                }
                )
        )

        assertEqualStructure (
            projection (
                crossProduct (
                    crossProduct (
                        selection (
                            extent (students),
                            (s: Rep[Student]) => s.firstName == "Sally"
                        ),
                        selection (
                            extent (registrations),
                            (r: Rep[Registration]) => r.comment == "This is an introductory Course"
                        )
                    ),
                    extent (courses)
                ),
                fun (
                    (sr_c: Rep[((Student, Registration), Course)]) => (sr_c._1._1, sr_c._1._2, sr_c._2)
                )
            ),
            query
        )
    }


    @Test
    def testCrossProduct3Selection1stAnd2ndAnd3rd () {
        val query = plan (
            SELECT (*) FROM(students, registrations, courses) WHERE (
                (s: Rep[Student], r: Rep[Registration], c: Rep[Course]) => {
                    s.firstName == "Sally" &&
                        r.comment == "This is an introductory Course" &&
                        c.title.startsWith ("Introduction")
                }
                )
        )

        assertEqualStructure (
            projection (
                crossProduct (
                    crossProduct (
                        selection (
                            extent (students),
                            (s: Rep[Student]) => s.firstName == "Sally"
                        ),
                        selection (
                            extent (registrations),
                            (r: Rep[Registration]) => r.comment == "This is an introductory Course"
                        )
                    ),
                    selection (
                        extent (courses),
                        (c: Rep[Course]) => c.title.startsWith ("Introduction")
                    )
                ),
                fun (
                    (sr_c: Rep[((Student, Registration), Course)]) => (sr_c._1._1, sr_c._1._2, sr_c._2)
                )
            ),
            query
        )
    }


    @Test
    def testCrossProduct3Selection1stAnd2ndAnd3rdInterleaved () {
        val query =
            plan (
                SELECT (*) FROM(students, registrations, courses) WHERE (
                    (s: Rep[Student], r: Rep[Registration], c: Rep[Course]) => {
                        c.title.startsWith ("Introduction") &&
                            r.comment == "This is an introductory Course" &&
                            s.firstName == "Sally" &&
                            c.creditPoints > 4 &&
                            s.lastName == "Fields" &&
                            r.studentMatriculationNumber > 100000
                    }
                    )
            )


        assertEqualStructure (
            projection (
                crossProduct (
                    crossProduct (
                        selection (
                            extent (students),
                            (s: Rep[Student]) =>
                                s.firstName == "Sally" &&
                                    s.lastName == "Fields"
                        ),
                        selection (
                            extent (registrations),
                            (r: Rep[Registration]) =>
                                r.comment == "This is an introductory Course" &&
                                    r.studentMatriculationNumber > 100000
                        )
                    ),
                    selection (
                        extent (courses),
                        (c: Rep[Course]) =>
                            c.title.startsWith ("Introduction") &&
                                c.creditPoints > 4
                    )
                ),
                fun (
                    (sr_c: Rep[((Student, Registration), Course)]) => (sr_c._1._1, sr_c._1._2, sr_c._2)
                )
            ),
            query
        )
    }


    @Test
    def testCrossProduct3Selection1stAnd2ndCombined () {
        val query =
            plan (
                SELECT (*) FROM(students, registrations, courses) WHERE (
                    (s: Rep[Student], r: Rep[Registration], c: Rep[Course]) => {
                        r.comment == "This is an introductory Course" ||
                            s.firstName == "Sally"
                    }
                    )
            )


        assertEqualStructure (
            projection (
                crossProduct (
                    selection (
                        crossProduct (
                            extent (students),
                            extent (registrations)
                        ),
                        (s: Rep[Student], r: Rep[Registration]) => {
                            r.comment == "This is an introductory Course" ||
                                s.firstName == "Sally"
                        }
                    ),
                    extent (courses)
                ),
                fun (
                    (sr_c: Rep[((Student, Registration), Course)]) => (sr_c._1._1, sr_c._1._2, sr_c._2)
                )
            ),
            query
        )
    }


    @Test
    def testCrossProduct3Selection1stAnd2ndAnd3ndCombined () {
        val query =
            plan (
                SELECT (*) FROM(students, registrations, courses) WHERE (
                    (s: Rep[Student], r: Rep[Registration], c: Rep[Course]) => {
                        r.comment == "This is an introductory Course" ||
                            c.creditPoints > 4 ||
                            s.firstName == "Sally"
                    }
                    )
            )


        assertEqualStructure (
            selection (
                crossProduct (
                    extent (students),
                    extent (registrations),
                    extent (courses)
                ),
                (s: Rep[Student], r: Rep[Registration], c: Rep[Course]) => {
                    r.comment == "This is an introductory Course" ||
                        c.creditPoints > 4 ||
                        s.firstName == "Sally"
                }
            ),
            query
        )
    }

    @Test
    def testJoin3On1stAnd2nd () {
        val query =
            plan (
                SELECT (*) FROM(students, registrations, courses) WHERE (
                    (s: Rep[Student], r: Rep[Registration], c: Rep[Course]) => {
                        s.matriculationNumber == r.studentMatriculationNumber
                    }
                    )
            )


        assertEqualStructure (
            projection (
                crossProduct (
                    equiJoin (
                        extent (students),
                        extent (registrations),
                        scala.List (
                            scala.Tuple2 (
                                fun((s: Rep[Student]) => s.matriculationNumber),
                                fun((r: Rep[Registration]) => r.studentMatriculationNumber)
                            )
                        )
                    ),
                    extent (courses)
                ),
                fun (
                    (sr_c: Rep[((Student, Registration), Course)]) => (sr_c._1._1, sr_c._1._2, sr_c._2)
                )
            ),
            query
        )
    }

    @Test
    def testJoin3On1stAnd2ndPlusOn2ndAnd3rd () {
        val query =
            plan (
                SELECT (*) FROM(students, registrations, courses) WHERE (
                    (s: Rep[Student], r: Rep[Registration], c: Rep[Course]) => {
                        s.matriculationNumber == r.studentMatriculationNumber &&
                            r.courseNumber == c.number
                    }
                    )
            )


        assertEqualStructure (
            projection (
                equiJoin (
                    equiJoin (
                        extent (students),
                        extent (registrations),
                        scala.List (
                            scala.Tuple2 (
                                fun((s: Rep[Student]) => s.matriculationNumber),
                                fun((r: Rep[Registration]) => r.studentMatriculationNumber)
                            )
                        )
                    ),
                    extent (courses),
                    scala.List (
                        scala.Tuple2 (
                            fun((s: Rep[Student], r: Rep[Registration]) => r.courseNumber),
                            fun((c: Rep[Course]) => c.number)
                        )
                    )
                ),
                fun (
                    (sr_c: Rep[((Student, Registration), Course)]) => (sr_c._1._1, sr_c._1._2, sr_c._2)
                )
            ),
            query
        )
    }
}