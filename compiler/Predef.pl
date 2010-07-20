/* License (BSD Style License):
   Copyright (c) 2010
   Department of Computer Science
   Technische Universität Darmstadt
   All rights reserved.

	Redistribution and use in source and binary forms, with or without
	modification, are permitted provided that the following conditions are met:

    - Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    - Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    - Neither the name of the Software Technology Group or Technische 
      Universität Darmstadt nor the names of its contributors may be used to 
      endorse or promote products derived from this software without specific 
      prior written permission.

	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
	AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
	IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
	ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
	LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
	CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
	SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
	INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
	ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
	POSSIBILITY OF SUCH DAMAGE.
*/


/* Definition of all operators and predicates that are either directly
	understood by SAE Prolog - i.e., that are taken care of during compilation -
	or which are pre-imlemented as part of SAE Prolog's library.
	
	The redefinition of built-in predicates (operators) is not supported. In 
	general, these predicates have the standard ISO semantics.

	@author Michael Eichberg
*/
:- module(
	'Compiler:Predef',
	[	predefined_predicates/2,
		predefined_operator/1,
		predefined_term_operator/1,
		predefined_arithmetic_operator/1,
		
		built_in_term/1,
		
		visualize_term_structure/2]
).




/* Definition of all predicates directly suppported by SAE Prolog including all
	relevant properties.

	@signature predefined_predicates(UserDefinedProgram,CompleteProgram)
	@param UserDefinedProgram is the AST of the loaded SAE Prolog program.
	@param CompleteProgram is the merge of the AST of the loaded program and the 
		built-in predicates.
*/
predefined_predicates(UserDefinedProgram,CompleteProgram) :-
	CompleteProgram = [
	
		% Primary Predicates - we need to analyze the sub
		pred(';'/2,[],[deterministic(no)]), % or
		%(not yet supported:) ('->'/2,[],[deterministic(dependent)]), % If->Then(;Else)
		%(not yet supported:) ('*->'/2,[],[deterministic(dependent)]), % Soft Cut
		pred(','/2,[],[deterministic(dependent)]), % and
	
		% Extra-logical Predicates
		pred(nonvar/1,[],[deterministic(yes)]),
		pred(var/1,[],[deterministic(yes)]),
		pred(integer/1,[],[deterministic(yes)]),
		pred(atom/1,[],[deterministic(yes)]),
		pred(compound/1,[],[deterministic(yes)]),
		pred(functor/3,[],[deterministic(yes)]),

		% "Other" Predicates
		pred('='/2,[],[deterministic(yes)]), % unify
		pred('\\='/2,[],[deterministic(yes)]), % does not unify
		pred('/'('\\+',1),[],[deterministic(yes)]), % not
		pred('=='/2,[],[deterministic(yes)]), % term equality
		pred('\\=='/2,[],[deterministic(yes)]), % term inequality

		pred('is'/2,[],[deterministic(yes),mode(-,+)]), % "arithmetic" assignment (comparison)

		pred(true/0,[],[deterministic(yes)]), % always succeeds
		pred(false/0,[],[deterministic(yes)]), % always fails
		pred(fail/0,[],[deterministic(yes)]), % always fails
		pred('!'/0,[],[deterministic(yes)]), % cut

		pred('<'/2,[],[deterministic(yes),mode(+,+)]), % arithmetic comparison; requires both terms to be instantiated
		pred('=:='/2,[],[deterministic(yes),mode(+,+)]), % arithmetic comparison; requires both terms to be instantiated
		pred('=<'/2,[],[deterministic(yes),mode(+,+)]), % arithmetic comparison; requires both terms to be instantiated
		pred('=\\='/2,[],[deterministic(yes),mode(+,+)]), % arithmetic comparison; requires both terms to be instantiated
		pred('>'/2,[],[deterministic(yes),mode(+,+)]), % arithmetic comparison; requires both terms to be instantiated
		pred('>='/2,[],[deterministic(yes),mode(+,+)]) % arithmetic comparison; requires both terms to be instantiated
		% The "other" arithmetic operators ( +, -, *,...) are not top-level predicates!
		
		| UserDefinedProgram].



/*	predefined_operator(Operator) :- succeeds if Operator is an Operator that is
	defined by SAE prolog.
*/
predefined_operator(Operator) :-
	predefined_term_operator(Operator);
	predefined_arithmetic_operator(Operator).




predefined_term_operator('=').
predefined_term_operator('\\=').
predefined_term_operator('==').
predefined_term_operator('\\==').




predefined_arithmetic_operator('<').
predefined_arithmetic_operator('=:=').
predefined_arithmetic_operator('=<').
predefined_arithmetic_operator('=\\=').
predefined_arithmetic_operator('>').
predefined_arithmetic_operator('>=').




/* built_in_term(Term) :- succeeds if Term is a term used internally by the
	SAE Prolog compiler to store meta-information.
	
	<p>
	Terms used internally by the SAE that could conflict with terms of SAE prolog
	programs always start with the '$SAE' "namespace" to avoid conflicts.<br/>
	The primary use case is to associate free variables of prolog clauses with
	names during compilation.	
	</p>
*/
built_in_term('$SAE':_).







number_term_nodes(Term,NumberedTerm) :-
	number_term_nodes(Term,1,_,NumberedTerm).

number_term_nodes(A,ID,NID,goal(ID,A)) :- var(A),!,NID is ID +1.
number_term_nodes((L,R),CID,NID,Goal) :- 
	!, 
	number_term_nodes(L,CID,IID,LGoal),
	number_term_nodes(R,IID,NID,RGoal),
	Goal = and(CID,IID,NID,(LGoal,RGoal)).
number_term_nodes((L;R),CID,NID,Goal) :- 
	!, 
	number_term_nodes(L,CID,IID,LGoal),
	number_term_nodes(R,IID,NID,RGoal),
	Goal = or(CID,IID,NID,(LGoal;RGoal)).
number_term_nodes(T,CID,NID,goal(CID,T)) :- NID is CID +1.


% to match the root node..
node_successors(Goal,NGoal) :- 
	Goal = goal(ID,G),!,
	Exit is ID + 1,
	is_cut(G,RelevantCutCall), % ... is not really required
	node_successors(Goal,Exit,Exit,Exit,RelevantCutCall,_Cuts,NGoal).
node_successors(Goal,NGoal) :- 
	Goal = and(_MinL,_MinR,Exit,(_LGoal,_RGoal)),!,
	node_successors(Goal,Exit,Exit,Exit,false,_Cuts,NGoal).	
node_successors(Goal,NGoal) :- 
	Goal = or(_MinL,_MinR,Exit,(_LGoal;_RGoal)),!,
	node_successors(Goal,Exit,Exit,Exit,false,_Cuts,NGoal).	
% to traverse the graph
node_successors(
		goal(ID,G),
		BranchSucceedsNID,
		BranchFailsNID,
		CutBranchFailsNID,
		RelevantCutCall,
		Cuts,
		NGoal) :- 
	!,
	(	is_cut(G) -> Cuts = true ; Cuts = RelevantCutCall ),
	NGoal = goal(ID,G,RelevantCutCall,BranchSucceedsNID,BranchFailsNID,CutBranchFailsNID).
node_successors(
		Goal,
		BranchSucceedsNID,
		BranchFailsNID,
		CutBranchFailsNID,
		RelevantCutCall, % in variable
		Cuts, % out variable
		NGoal) :-
	Goal = and(MinL,MinR,NID,(LGoal,RGoal)),!,
	node_successors(LGoal,MinR,BranchFailsNID,CutBranchFailsNID,RelevantCutCall,LCuts,NLGoal),
	% if there are no (open) choicepoints when this goal is reached, then it
	% is irrelevant if in the left branch a cut is encountered, as such a cut will never
	% cut of any choice points created (later) in the right branch
	( (BranchFailsNID =:= CutBranchFailsNID,!, RCC = false) ; RCC=LCuts ),
	node_successors(RGoal,BranchSucceedsNID,BranchFailsNID,CutBranchFailsNID,RCC,Cuts,NRGoal),
	NGoal = and(MinL,MinR,NID,(NLGoal,NRGoal)).
node_successors(
		Goal,
		BranchSucceedsNID,
		BranchFailsNID,
		CutBranchFailsNID,
		RelevantCutCall, % in variable
		Cuts, % out variable
		NGoal) :-
	Goal = or(MinL,MinR,NID,(LGoal;RGoal)),!,
	% when we reach this node (this "or" goal), a choice point is created and 
	% the right branch is the target if the 
	% evaluation of the left branch fails (and unless another cut is encountered...)
	% hence, "RelevantCutCall" is set to false because (at least) this choice point
	% is not cut
	node_successors(LGoal,BranchSucceedsNID,MinR,CutBranchFailsNID,false,LCuts,NLGoal),
	% the right branch is only evaluated if not cut is called when
	% the left branch is evaluated; however, previous choice points may be
	% cut and hence, when evaluating the right branch we have to the previous
	% information whether the cut was called or not into consideration
	node_successors(RGoal,BranchSucceedsNID,BranchFailsNID,CutBranchFailsNID,RelevantCutCall,RCuts,NRGoal),
	(
			(LCuts = true, RCuts = true,!,Cuts = true)
		;
			(LCuts = false, RCuts = false,!,Cuts = false)
		;
			Cuts = maybe
	),
	NGoal = or(MinL,MinR,NID,(NLGoal;NRGoal)).


is_cut(Term) :- nonvar(Term),Term = !.

is_cut(Term,Cut) :- is_cut(Term),!,Cut=true.
is_cut(_Term,false).



generate_dot_visualization(Term,OutputFile) :-
	visualize_term_structure(Term,DotFile),
	open(OutputFile,write,Stream),
	write(Stream,DotFile),
	close(Stream).


/*
	Examples:
	(G=(a,(b,(c,!,d,e;f);g),h)),normalize_goal_sequence_order(G,NG),visualize_term_structure(NG,VG),write(VG).

	Does not contain any relevant cut calls...
	generate_dot_visualization(((b;a,!),c;f),'/Users/Michael/Desktop/FlexibleGoalSequence.dot').
	generate_dot_visualization(call(a1,a2,a3),'/Users/Michael/Desktop/Forward.dot').
	generate_dot_visualization((((ap,!,aa);(bp,!,ba);(cp,!,ca)),f),'/Users/Michael/Desktop/Switch.dot').
	generate_dot_visualization((!,a,b,c,(d;e),f),'/Users/Michael/Desktop/Standard.dot').
	generate_dot_visualization((!),'/Users/Michael/Desktop/JustCut.dot').
	generate_dot_visualization((a,(b,!,c;d);f),'/Users/Michael/Desktop/TermWithCut.dot').
	generate_dot_visualization((((a;(c,!)),b),VG),'/Users/Michael/Desktop/NoRelevantCutCalls.dot').
*/
visualize_term_structure(Term,DotFile) :- 
	number_term_nodes(Term,NT),
	node_successors(NT,NTwithSucc),
	visualize_numbered_term(NTwithSucc,[],DF),
	atomic_list_concat(DF,T),
	term_to_atom(Term,A),
	numbered_term_exit_node_id(NTwithSucc,EID),
	atomic_list_concat([
		'digraph G {\n',
		'label="Term: ',A,'";\nnode [shape=none];\nedge [arrowhead=none];\n',
		T,
		EID,' [label="Exit",shape="Box"];\n',
		'}'],DotFile).




visualize_numbered_term(ThisGoal,CurrentDotFile,DotFile) :- 
	ThisGoal = and(_MinL,_MinR,_Max,(LGoal,RGoal)),!,
	visualize_numbered_term(LGoal,CurrentDotFile,IDF1),numbered_term_node_idatom(LGoal,LID),
	visualize_numbered_term(RGoal,IDF1,IDF2),numbered_term_node_idatom(RGoal,RID),	
	numbered_term_node_idatom(ThisGoal,TID),
	DotFile=[
		TID,' [label="⋀ (and)"];\n',
		TID,' -> ',LID,' [penwidth="2.25"];\n',
		TID,' -> ',RID,' [penwidth="2.25"];\n'
		|IDF2].
visualize_numbered_term(ThisGoal,CurrentDotFile,DotFile) :- 
	ThisGoal = or(_MinL,_MinR,_Max,(LGoal;RGoal)),!,
	visualize_numbered_term(LGoal,CurrentDotFile,IDF1),numbered_term_node_idatom(LGoal,LID),
	visualize_numbered_term(RGoal,IDF1,IDF2),numbered_term_node_idatom(RGoal,RID),	
	numbered_term_node_idatom(ThisGoal,TID),
	DotFile=[
		TID,' [label="⋁ (or)"];\n',
		TID,' -> ',LID,' [penwidth=2.25];\n',
		TID,' -> ',RID,' [penwidth=2.25];\n'	
		|IDF2].
visualize_numbered_term(goal(V,G,RelevantCutCall,SID,FID,CFID),CurrentDotFile,DotFile) :-
	term_to_atom(G,A),
	(
		( 
			RelevantCutCall = true,!,
			atomic_list_concat([
					V,' [label="',A,'",fontcolor=maroon];\n',
					V,' -> ',SID,' [color=green,constraint=false,arrowhead=normal];\n',
					V,' -> ',CFID,' [label="!",color=maroon,constraint=false,arrowhead=normal];\n'
				],ID)
		)
		;
		( 
			RelevantCutCall = false,!,
			atomic_list_concat([
					V,' [label="',A,'"];\n',
					V,' -> ',SID,' [color=green,constraint=false,arrowhead=normal];\n',
					V,' -> ',FID,' [color=red,constraint=false,arrowhead=normal];\n'
				],ID)
		)
		;
		(
			RelevantCutCall = maybe,!,
			atomic_list_concat([
					V,' [label="',A,'",fontcolor=darksalmon];\n',
					V,' -> ',SID,' [color=green,constraint=false,arrowhead=normal];\n',
					V,' -> ',FID,' [color=salmon,constraint=false,arrowhead=normal];\n',
					V,' -> ',CFID,' [label="!",color=pink,constraint=false,arrowhead=normal];\n'	
				],ID)		
		)
	),
	DotFile=[ID|CurrentDotFile].




numbered_term_node_idatom(and(MinL,MinR,Max,_),ID) :- atomic_list_concat([and,'_',MinL,'_',MinR,'_',Max],ID).
numbered_term_node_idatom(or(MinL,MinR,Max,_),ID) :- atomic_list_concat([or,'_',MinL,'_',MinR,'_',Max],ID).
numbered_term_node_idatom(goal(ID,_G,_RelevantCutCall,_SID,_FID,_CFID),ID).




numbered_term_exit_node_id(goal(_ID,_G,_RelevantCutCall,EID,EID,EID),EID) :- !.
numbered_term_exit_node_id(and(_MinL,_MinR,Max,_),Max) :- !.
numbered_term_exit_node_id(or(_Min,_MinR,Max,_),Max) :- !.
