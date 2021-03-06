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
package idb.algebra.opt

import idb.algebra.ir.RelationalAlgebraIRBasicOperators
import idb.lms.extensions.FunctionUtils
import idb.lms.extensions.functions.TupledFunctionsExpDynamicLambda

/**
 * Rules for lifting projections higher up in the operator tree
 *
 * @author Ralf Mitschke
 *
 */
trait RelationalAlgebraIROptLiftProjection
    extends RelationalAlgebraIRBasicOperators
    with TupledFunctionsExpDynamicLambda
    with FunctionUtils
{


    /**
     * Lifting projections over cross products simplifies all further treatment of cross products and joins,
     * e.g., re-orderings etc., since there are less nodes between the cross products and joins
     */
    override def crossProduct[DomainA: Manifest, DomainB: Manifest] (
        relationA: Rep[Query[DomainA]],
        relationB: Rep[Query[DomainB]]
    ): Rep[Query[(DomainA, DomainB)]] =
        (relationA, relationB) match {
            case (Def (Projection (ra, fa)), Def (Projection (rb, fb))) =>
                projection (
                    crossProduct (
                        ra,
                        rb
                    )(domainOf (ra), domainOf (rb)),
                    fun (
                        (x: Rep[Any], y: Rep[Any]) => (fa (x), fb (y))
                    )(
                        parameterManifest (parameter (fa)), // dynamic manifests for x
                        parameterManifest (parameter (fb)), // dynamic manifests for y
                        manifest[(DomainA, DomainB)]
                    )
                )

            case (Def (Projection (ra, fa)), rb) =>
                projection (
                    crossProduct (
                        ra,
                        rb
                    )(domainOf (ra), manifest[DomainB]),
                    fun (
                        (x: Rep[Any], y: Rep[DomainB]) => (fa (x), y)
                    )(
                        parameterManifest (parameter (fa)), // dynamic manifests for x
                        manifest[DomainB],
                        manifest[(DomainA, DomainB)]
                    )
                )


            case (ra, Def (Projection (rb, fb))) => {
                val newProjection =        fun (
                    (x: Rep[DomainA], y: Rep[Any]) => (x, fb (y))
                )(
                    manifest[DomainA],
                    parameterManifest (parameter (fb)), // dynamic manifests for y
                    manifest[(DomainA, DomainB)]
                )

                projection (
                    crossProduct (
                        ra,
                        rb
                    )(manifest[DomainA], domainOf (rb)),
                    newProjection

                )
            }
            case _ => super.crossProduct (relationA, relationB)
        }

}


