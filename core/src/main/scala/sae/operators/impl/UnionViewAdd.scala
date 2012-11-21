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
package sae.operators.impl

import sae.{Observable, Observer, Relation}
import sae.operators.Union
import sae.deltas.{Update, Deletion, Addition}

/**
 * A self maintained union, that produces count(A) + count(B) duplicates for underlying relations A and B
 */
class UnionViewAdd[Range, DomainA <: Range, DomainB <: Range](val left: Relation[DomainA],
                                                              val right: Relation[DomainB])
        extends Union[Range, DomainA, DomainB]
        with Observer[Range]
{
    left addObserver this

    right addObserver this

    override protected def childObservers(o: Observable[_]): Seq[Observer[_]] = {
        if (o == left || o == right) {
            return List(this)
        }
        Nil
    }

    /**
     * Applies f to all elements of the view.
     */
    def foreach[T](f: (Range) => T) {
        left.foreach(f)
        right.foreach(f)
    }

    def added(v: Range) {
        element_added(v)
    }

    def removed(v: Range) {
        element_removed(v)
    }

    def updated(oldV: Range, newV: Range) {
        element_updated(oldV, newV)
    }

    def updated[U <: Range](update: Update[U]) {
        element_updated(update)
    }

    def modified[U <: Range](additions: Set[Addition[U]], deletions: Set[Deletion[U]], updates: Set[Update[U]]) {
        element_modifications(additions, deletions, updates)
    }
}
