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

import saere.PredicateInstanceFactory;
import saere.PredicateRegistry;
import saere.Solutions;
import saere.StringAtom;
import saere.Term;

/**
 * Implementation of ISO Prolog's <code>repeat/0</code> predicate.
 * 
 * @author Michael Eichberg
 */
public final class Repeat0 implements Solutions {

	// ?- repeat,write(x),fail.
	// xxxxxxxx.....xxxxxx

	// ?- repeat,write(x),!,fail.
	// x
	// false.

	public static void registerWithPredicateRegistry(PredicateRegistry registry) {

		registry.register(StringAtom.instance("repeat"), 0,
				new PredicateInstanceFactory() {

					@Override
					public Solutions createPredicateInstance(Term[] args) {
						return REPEAT0;
					}
				});

	}

	public final static Repeat0 REPEAT0 = new Repeat0();

	public Repeat0() {
		// nothing to do
	}

	@Override
	public boolean next() {
		return true;
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
