/* License (BSD Style License):
 * Copyright (c) 2010
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
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
package saere.predicate;

import saere.*;

/**
 * Implementation of ISO Prolog's not (<code>\+</code>) operator.
 * 
 * <pre>
 * <code>
 * ?- X=4,\+ ( (member(X,[1,2,3])) ),write(X).
 * 4
 * X = 4.
 * ?- X=3,\+ ( (member(X,[1,2,3])) ),write(X).
 * false.
 * ?- not( (member(X,[1,2,3]),false) ).
 * true.
 * ?- repeat,not( (member(X,[1,2,3]),false,!) ).
 * true ;
 * true ;
 * ...
 * true .
 * ?- not( (member(X,[1,2,3]),false) ),write(X).
 * _G248
 * true.
 * </code>
 * </pre>
 * 
 * @author Michael Eichberg
 */
public final class Not1 implements Solutions {

	public static void registerWithPredicateRegistry(PredicateRegistry registry) {

		PredicateInstanceFactory pif = new PredicateInstanceFactory() {

			@Override
			public Solutions createPredicateInstance(Term[] args) {
				return new Not1(args[0]);
			}
		};
		registry.register(StringAtom.NOT_FUNCTOR, 1, pif);
		registry.register(StringAtom.NOT_OPERATOR_FUNCTOR, 1, pif);

	}

	private final Term t;

	private boolean called = false;

	public Not1(final Term t) {

		this.t = t;
	}

	@Override
	public boolean next() {
		if (!called) {
			called = true;
			final Solutions s = t.call();
			final boolean succeeded = s.next();
			if (succeeded) {
				s.abort();
				return false;
			}
			else {
				return true;
			}
		} else {
			return false;
		}
	}

	@Override
	public void abort() {
		// nothing to do
	}

	@Override
	public boolean choiceCommitted() {
		return false;
	}
}
