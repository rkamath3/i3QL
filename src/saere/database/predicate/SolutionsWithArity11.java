package saere.database.predicate;

import java.util.Iterator;

import saere.Solutions;
import saere.State;
import saere.Term;
import saere.database.DatabaseAdapter;

public final class SolutionsWithArity11 implements Solutions {

	private final Iterator<Term> iterator;
	private final Term arg0;
	private final Term arg1;
	private final Term arg2;
	private final Term arg3;
	private final Term arg4;
	private final Term arg5;
	private final Term arg6;
	private final Term arg7;
	private final Term arg8;
	private final Term arg9;
	private final Term arg10;
	private final State s0;
	private final State s1;
	private final State s2;
	private final State s3;
	private final State s4;
	private final State s5;
	private final State s6;
	private final State s7;
	private final State s8;
	private final State s9;
	private final State s10;
	
	private int progress;
	
	public SolutionsWithArity11(DatabaseAdapter adapter, Term query) {
		assert query.arity() == 11 : "Invalid query arity " + query.arity();
		
		arg0 = query.arg(0);
		arg1 = query.arg(1);
		arg2 = query.arg(2);
		arg3 = query.arg(3);
		arg4 = query.arg(4);
		arg5 = query.arg(5);
		arg6 = query.arg(6);
		arg7 = query.arg(7);
		arg8 = query.arg(8);
		arg9 = query.arg(9);
		arg10 = query.arg(10);
		
		// save original states
		s0 = arg0.manifestState();
		s1 = arg1.manifestState();
		s2 = arg2.manifestState();
		s3 = arg3.manifestState();
		s4 = arg4.manifestState();
		s5 = arg5.manifestState();
		s6 = arg6.manifestState();
		s7 = arg7.manifestState();
		s8 = arg8.manifestState();
		s9 = arg9.manifestState();
		s10 = arg10.manifestState();
		
		iterator = adapter.query(query);
	}

	public boolean next() {

		// restore old states
		reset();
		
		while (iterator.hasNext()) {
			Term fact = iterator.next();
			
			// attempt unification...
			if (fact.arity() == 3) {
				progress++;
				if (arg0.unify(fact.arg(0))) {
					progress++;
					if (arg1.unify(fact.arg(1))) {
						progress++;
						if (arg2.unify(fact.arg(2))) {
							progress++;
							if (arg3.unify(fact.arg(3))) {
								progress++;
								if (arg4.unify(fact.arg(4))) {
									progress++;
									if (arg5.unify(fact.arg(5))) {
										progress++;
										if (arg6.unify(fact.arg(6))) {
											progress++;
											if (arg7.unify(fact.arg(7))) {
												progress++;
												if (arg8.unify(fact.arg(8))) {
													progress++;
													if (arg9.unify(fact.arg(9))) {
														progress++;
														if (arg10.unify(fact.arg(10))) {
															return true;
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			reset();
		}
		
		return false;
	}
	
	// Reset only where necessary
	private void reset() {
		if (progress > 0) arg0.setState(s0);
		if (progress > 1) arg1.setState(s1);
		if (progress > 2) arg2.setState(s2);
		if (progress > 3) arg3.setState(s3);
		if (progress > 4) arg4.setState(s4);
		if (progress > 5) arg5.setState(s5);
		if (progress > 6) arg6.setState(s6);
		if (progress > 7) arg7.setState(s7);
		if (progress > 8) arg8.setState(s8);
		if (progress > 9) arg9.setState(s9);
		if (progress > 10) arg10.setState(s10);
		progress = 0;
	}
}