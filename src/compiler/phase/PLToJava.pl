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

/*
	Generates the code for the SAE program.

	@author Michael Eichberg
*/
:- module('SAEProlog:Compiler:Phase:PLtoOO',[pl_to_java/4]).

:- use_module('../AST.pl').
:- use_module('../Predef.pl').
:- use_module('../Utils.pl').
:- use_module('../Debug.pl').



/**
	Encoding of the SAE Prolog program in Java.

	@param Debug the list of debug information that should be printed.	
*/
pl_to_java(DebugConfig,Program,OutputFolder,Program) :-
	debug_message(DebugConfig,on_entry,write('\n[Debug] Phase: Generate the Java Program____________________________________\n')),
	( exists_directory(OutputFolder) ; make_directory(OutputFolder) ),
	working_directory(Old,OutputFolder),
	( exists_directory(predicates) ; make_directory(predicates) ),
	working_directory(_,predicates),!,
	(	/* 
			Loop over all user defined predicates and generate the code for each 
		 	predicate; user_predicate is the loop anchor, it succeeds for each 
		 	user_predicate.
		*/
		user_predicate(Program,Predicate),
		predicate_identifier(Predicate,PredicateIdentifier),
		term_to_atom(PredicateIdentifier,PredicateIdentifierAtom),
		debug_message(DebugConfig,processing_predicate,write_atomic_list(['[Debug] Processing Predicate: ',PredicateIdentifierAtom,'\n'])),		
		process_predicate(Predicate),
		fail % i.e., continue with the next predicate
	;	
		true
	),
	working_directory(_,Old).



process_predicate(Predicate) :-
	predicate_identifier(Predicate,Functor/Arity),
	main_template(Functor,Arity,Template,Core), %IMPROVE ClauseBodies and Methods don't quite describe it...

	predicate_clauses(Predicate,Clauses),
	
	create_clause_branches(Clauses,ClauseBranches),
	NextMethod = [
	'	public boolean next(){\n',
	'		switch(clauseToExecute){\n',
	ClauseBranches,
	'		}\n',
	'	}\n'
	],
	atomic_list_concat(NextMethod,Core),
	
	% concatenate all parts and write out the Java file
	atomic_list_concat(Template,TheCode),
	atomic_list_concat([Functor,Arity,'.java'],FileName),
	open(FileName,write,Stream),
	write(Stream,TheCode),
	close(Stream).



main_template(Functor,Arity,Template,Core) :-
	call_foreach_i_in_0_to_u(Arity,variable_for_term_i,TermVariables),
	atomic_list_concat(TermVariables,';\n\t',ConcatenatedTermVariables),
	
	call_foreach_i_in_0_to_u(Arity,variable_initialization_for_term_i,TermVariablesInitializations),
	atomic_list_concat(TermVariablesInitializations,';\n\t\t',ConcatenatedTermVariablesInitializations),	
	
	call_foreach_i_in_0_to_u(Arity,args_i_access,Args),
	atomic_list_concat(Args,',',ConcatenatedArgs),
	
	call_foreach_i_in_0_to_u(Arity,constructor_arg_for_term_i,ConstructorArgs),
	atomic_list_concat(ConstructorArgs,',',ConcatenatedConstructorArgs),
	
	Template = [
		'// Generated by SAE Prolog - www.opal-project.de\n',
		'package predicates;\n\n',
		'import saere.*;\n',
		'import static saere.term.TermFactory.*;\n\n',
		'public final class ',Functor,Arity,' implements Solutions {\n',
		'\n',
		/*
		'	public static void registerWithPredicateRegistry(PredicateRegistry registry) {\n',
		'		registry.register(\n',
		'			StringAtom.instance("',Functor , '"),\n',
		'			',Arity,',\n',
		'			new PredicateInstanceFactory() {\n',
		'				@Override\n',
		'				public Solutions createPredicateInstance(Term[] args) {\n',
		'					return new ',Functor,Arity,'(',ConcatenatedArgs,');\n',
		'				}\n',
		'			}\n',
		'		);\n',
		'	}\n\n',
		*/
		'	public final static PredicateIdentifier IDENTIFIER = new PredicateIdentifier(StringAtom.instance("',Functor,'"), ',Arity,');\n\n',
		'	public final static PredicateFactory FACTORY = new NArgsPredicateFactory() {\n',
		'		@Override\n',
		'		public Solutions createInstance(Term[] args) {\n',
		'			return new ',Functor,Arity,'(',ConcatenatedArgs,');\n',
		'		}\n',
		'	};\n\n',
		'	public static void registerWithPredicateRegistry(PredicateRegistry registry) {\n',
		'		registry.register(IDENTIFIER, FACTORY);\n',
		'	}\n\n',		
		'	private int clauseToExecute = 0;\n',
%		'	private Term clauseImplementation;\n',
		'	private Solutions clauseSolutions;\n',
		'\n',
		'	',ConcatenatedTermVariables,';\n\n',
		'	public ',Functor,Arity,'(',ConcatenatedConstructorArgs,'){\n',
		'		',ConcatenatedTermVariablesInitializations,';\n',
		'	}\n\n',
		Core,'\n',
		'	@Override\n',
		'	public void abort() {\n',
		'		clauseSolutions.abort();\n',
		'		clauseSolutions = null;\n',
%		'		clauseImplementation = null;\n',				
		'	}\n\n',			
		'	@Override\n',
		'	public boolean choiceCommitted() {\n',
		'		return false;\n',
		'	}\n',
		'}\n'
	].
	
	
	
args_i_access(N,Args)	 :- atomic_list_concat(['args[',N,']'],Args).

variable_for_term_i(N,TermVariable)	 :- atomic_list_concat(['private final Term arg',N],TermVariable).

variable_initialization_for_term_i(N,TermVariableInitialization)	 :- atomic_list_concat(['this.arg',N,' = arg',N],TermVariableInitialization).

constructor_arg_for_term_i(I,ConstructorArg) :- atom_concat('final Term arg',I,ConstructorArg).


create_clause_branches(Clauses,ConcatenatedClauseBranches) :- 
	create_clause_branches(Clauses,0,ClauseBranches),
	atomic_list_concat(ClauseBranches,ConcatenatedClauseBranches).

/**
	@signature create_clause_branches(Clauses,Id,ClauseBranches)
*/
create_clause_branches(NoMoreClauses,Id,[FailCase]) :- 
	var(NoMoreClauses),!, % Clauses is an open list...
	atomic_list_concat([
	'\t\tdefault :\n',
%	'\t\t\tclauseImplementation = null;\n',
	'\t\t\tclauseSolutions = null;\n',
	'\t\t\treturn false;\n'
	],FailCase).
create_clause_branches([Clause|Clauses],Id,[ConcatenatedClauseBranch|ClausesBranches]) :- !,
	InitBranch = Id,
	SolutionBranch is InitBranch + 1,
	clause_definition(Clause,ClauseDefinition),
	rule_body(ClauseDefinition,Body),
	create_term(Body,BodyConstructor,Variables),
	rule_head(ClauseDefinition,Head),
	create_clause_variables(Head,Variables,ClauseVariables),
	(
		nonvar(Clauses), % Clauses is an open list...
		atomic_list_concat(
			[
			'\t\t\tif (clauseSolutions.choiceCommitted()){\n',
%			'\t\t\t\tclauseImplementation = null;\n', 
			'\t\t\t\tclauseSolutions = null;\n', 	
			'\t\t\t\treturn false;\n',
			'\t\t\t}\n'
			],
			NotSucceeded
		)
	;
		NotSucceeded = '\t\t\t// fall through...\n'
	),!,
	ClauseBranch = [
	'\t\tcase ',Id,': {\n',
	'\t\t\t',ClauseVariables,'\n',
%	'\t\t\tclauseImplementation = ',BodyConstructor,';\n',
	'\t\t\tclauseSolutions = ',BodyConstructor,'.call();\n',
%	'\t\t\tclauseSolutions = clauseImplementation.call();\n',
	'\t\t\tclauseToExecute = ',SolutionBranch,';\n',
	'\t\t}\n',
	'\t\tcase ',SolutionBranch,':{\n',
	'\t\t\tboolean succeeded = clauseSolutions.next();\n',
	'\t\t\tif (succeeded) return true;\n',
	NotSucceeded,
	'\t\t}\n'
	],
	atomic_list_concat(ClauseBranch,ConcatenatedClauseBranch),
	NewId is Id + 2,
	create_clause_branches(Clauses,NewId,ClausesBranches).
	



/**
	@signature create_clause_variables(Head,BodyVariables,ClauseVariables).
*/
create_clause_variables(Head,BodyVariables,ClauseVariablesDefinitions) :-
	variables_of_term(Head,HeadVariables),
	create_clause_variable(HeadVariables,BodyVariables,ClauseVariables),
	atomic_list_concat(ClauseVariables,'\n\t\t\t',ConcatenatedClauseVariables),
	create_arg_head_variables_mapping(0,HeadVariables,HeadVariablesMapping),
	atomic_list_concat(HeadVariablesMapping,'\n\t\t\t',ConcatenatedHeadVariablesMapping),
	atomic_list_concat([
		ConcatenatedHeadVariablesMapping,'\n\t\t\t',
		ConcatenatedClauseVariables],
		ClauseVariablesDefinitions).


create_clause_variable(HeadVariables,[BodyVariable|BodyVariables],ClauseVariables) :-
	(
		\+ memberchk(BodyVariable,HeadVariables),
		atomic_list_concat(['Variable ',BodyVariable,' = variable();'],ClauseVariable),
		ClauseVariables = [ClauseVariable|FurhterClauseVariables],
		create_clause_variable(HeadVariables,BodyVariables,FurhterClauseVariables)
	;
		create_clause_variable(HeadVariables,BodyVariables,ClauseVariables)
	),!.
create_clause_variable(_,[],[]).



create_arg_head_variables_mapping(Id,[HeadVariable|HeadVariables],[HeadVariablesMapping|HeadVariablesMappings]) :- !,
	atomic_list_concat(['Term ',HeadVariable,' = ','arg',Id,';'],HeadVariablesMapping),
	NewId is Id + 1,
	create_arg_head_variables_mapping(NewId,HeadVariables,HeadVariablesMappings).
create_arg_head_variables_mapping(_Id,[],[]).	



create_term(ASTNode,TermConstructor,Variables) :-
	create_term(ASTNode,TermConstructor,[],Variables).
/**
	@signature create_term(ASTNode,TermConstructor,OldVariables,NewVariables)
*/
create_term(ASTNode,TermConstructor,Vs,Vs) :-	
	integer_atom(ASTNode,Value),!,
%	write(ASTNode),write(' '),write(integer_atom),write(' '),write(Value),	
	atomic_list_concat(['atomic(',Value,')'],TermConstructor).
create_term(ASTNode,TermConstructor,Vs,Vs) :-
	float_atom(ASTNode,Value),!,
%write(float_atom),write(' '),write(value),
	atomic_list_concat(['atomic(',Value,')'],TermConstructor).
create_term(ASTNode,TermConstructor,Vs,Vs) :-	
	string_atom(ASTNode,Value),!,
%	write(string_atom),write(' '),write(value),
	(
		predefined_string_atom_constructors(Value,TermConstructor)
%		Value = '!', TermConstructor = 'cut()'
	;
		atomic_list_concat(['atomic("',Value,'")'],TermConstructor)
	),!.
create_term(ASTNode,VariableName,OldVs,NewVs) :- 
	variable(ASTNode,VariableName),!,
	add_to_set(VariableName,OldVs,NewVs).	
create_term(ASTNode,'variable()',Vs,Vs) :- anonymous_variable(ASTNode,_VariableName),!.	
create_term(ASTNode,TermConstructor,OldVs,NewVs) :-
	complex_term(ASTNode,Functor,Args),
	(	
		Args = [ArgASTNode],create_term(ArgASTNode,ArgTermConstructor,OldVs,NewVs),
		atomic_list_concat(['compoundTerm("',Functor,'",',ArgTermConstructor,')'],TermConstructor)
	;
		Args = [LASTNode,RASTNode],
		create_term(LASTNode,LTermConstructor,OldVs,IVs),
		create_term(RASTNode,RTermConstructor,IVs,NewVs),
		(
			Functor = ',',
			atomic_list_concat(['and(',LTermConstructor,',',RTermConstructor,')'],TermConstructor)
		;	
			Functor = ';',
			atomic_list_concat(['or(',LTermConstructor,',',RTermConstructor,')'],TermConstructor)
		;	
			Functor = '=',
			atomic_list_concat(['unify(',LTermConstructor,',',RTermConstructor,')'],TermConstructor)

		;
			(
				predefined_string_atom_constructors(Functor,CompoundTermFunctorConstructor)
			;
				atomic_list_concat(['StringAtom.instance("',Functor,'")'],CompoundTermFunctorConstructor)
			),
			atomic_list_concat(['compoundTerm(',CompoundTermFunctorConstructor,',',LTermConstructor,',',RTermConstructor,')'],TermConstructor)
		)
	;
		create_terms(Args,TermConstructors,OldVs,NewVs),
		atomic_list_concat(TermConstructors,',',TermConstructorArgs),
		atomic_list_concat(['compoundTerm(StringAtom.instance("',Functor,'"),',TermConstructorArgs,')'],TermConstructor)		
	),!.

create_terms([Arg|Args],[TermConstructor|TermConstructors],OldVs,NewVs) :- !,
	create_term(Arg,TermConstructor,OldVs,IVs),
	create_terms(Args,TermConstructors,IVs,NewVs).
create_terms([],[],Vs,Vs).
	


predefined_string_atom_constructors('\\=','StringAtom.DOES_NOT_UNIFY_FUNCTOR').
predefined_string_atom_constructors('!','StringAtom.CUT_FUNCTOR').
predefined_string_atom_constructors('\\+','StringAtom.NOT_OPERATOR_FUNCTOR').
predefined_string_atom_constructors('not','StringAtom.NOT_FUNCTOR').
predefined_string_atom_constructors('.','StringAtom.LIST_FUNCTOR').
predefined_string_atom_constructors('[]','StringAtom.EMPTY_LIST_FUNCTOR').
predefined_string_atom_constructors('+','StringAtom.PLUS_FUNCTOR').
predefined_string_atom_constructors('-','StringAtom.MINUS_FUNCTOR').
predefined_string_atom_constructors('-','StringAtom.MULT_FUNCTOR').
predefined_string_atom_constructors('is','StringAtom.IS_FUNCTOR').
predefined_string_atom_constructors('<','StringAtom.ARITH_SMALLER_THAN_FUNCTOR').
predefined_string_atom_constructors('=:=','StringAtom.ARITH_IS_EQUAL_FUNCTOR').
predefined_string_atom_constructors('=\\=','StringAtom.ARITH_IS_NOT_EQUAL_FUNCTOR').
	