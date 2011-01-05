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



/**
	Generates the code for the SAE program.

	@author Michael Eichberg
*/
:- module('SAEProlog:Compiler:Phase:OOtoJava',[oo_to_java/4]).

:- use_module('../AST.pl').
:- use_module('../Predef.pl').
:- use_module('../Utils.pl').
:- use_module('../Debug.pl').



/**
	Encodes an SAE Prolog program's object-oriented representation in Java.

	@param Debug the list of debug information that should be printed.	
*/
oo_to_java(DebugConfig,Program,OutputFolder,Program) :-
	debug_message(DebugConfig,on_entry,write('\n[Debug] Phase: Generate the Java Program____________________________________\n')),
	( exists_directory(OutputFolder) ; make_directory(OutputFolder) ),
	working_directory(Old,OutputFolder),
	( exists_directory(predicates) ; make_directory(predicates) ),
	working_directory(_,predicates),!,
	% the code generation phase does not affect the AST whatsoever; but, if an
	% error is encountered, we want to make sure that the output directory is 
	% still reset correctly.
	catch(
		foreach_user_predicate(Program,process_predicate(DebugConfig,Program)),
		E,
		(
			working_directory(_,Old),
			throw(E)
		)
	),
	working_directory(_,Old),!.



process_predicate(DebugConfig,_Program,Predicate) :-
	predicate_identifier(Predicate,PredicateIdentifier),
	term_to_atom(PredicateIdentifier,PredicateIdentifierAtom),
	debug_message(DebugConfig,processing_predicate,write_atomic_list(['[Debug] Processing Predicate: ',PredicateIdentifierAtom,'\n'])),
	% implementation ...
	predicate_meta(Predicate,Meta),
	lookup_in_meta(oo_ast(AST),Meta),
	serialize_ast_nodes_to_java(AST).



serialize_ast_nodes_to_java([]).
serialize_ast_nodes_to_java([Node|Nodes]) :-
	gen_code(Node),
	serialize_ast_nodes_to_java(Nodes).



gen_code(class_decl(PredicateIdentifier,InheritedInterface,ClassMembers)) :- !,
	PredicateIdentifier = Functor/Arity,
	atomic_list_concat([Functor,Arity,'.java'],FileName),
	open(FileName,write,Stream),
	write_class_decl(Stream,PredicateIdentifier,InheritedInterface,ClassMembers),
	close(Stream).

gen_code(predicate_registration(PredicateIdentifier)) :- !,
	PredicateIdentifier = Functor/Arity,
	atomic_list_concat([Functor,Arity,'Factory.java'],FileName),
	open(FileName,write,Stream),
	write_predicate_registration(Stream,PredicateIdentifier),
	close(Stream).

gen_code(X) :- throw(internal_error(gen_code/1,['unknown OO AST node:',X])).



/* ************************************************************************** *\
 *                                                                            *
 *               P R E D I C A T E   I M P L E M E N T A T I O N              *
 *                                                                            *
\* ************************************************************************** */

write_class_decl(Stream,Functor/Arity,InheritedInterface,ClassMembers) :-
	write_atomic_list(
		Stream,
		[
		'/*\n',
		' * Generated by SAE Prolog - www.opal-project.de\n',
		' * \n',	
		' * DO NOT CHANGE MANUALLY - THE CLASS WILL COMPLETELY BE REGENERATED\n',			
		' */\n',
		'package predicates;\n',
		'import saere.*;\n',
		'import saere.predicate.*;\n',		
		'import saere.term.*;\n\n',
		'import static saere.term.Terms.*;\n',
		'import static saere.IntValue.*;\n',
		'import static saere.StringAtom.*;\n\n',
		'public final class ',Functor,Arity,' '
		]
	),
	write_inherited_interface(Stream,InheritedInterface),
	write(Stream,'{\n'),
	write_class_members(Stream,ClassMembers),
	write(Stream,'}\n').


write_inherited_interface(_Stream,none) :- !.
write_inherited_interface(Stream,Type) :- !,
	write(Stream,'implements '),write_type(Stream,Type).
write_inherited_interface(_Stream,X) :-  
	throw(internal_error(write_inherited_interface/2,['inheritance of an unknown type:',X])).	



write_class_members(_Stream,[]).
write_class_members(Stream,[ClassMember|ClassMembers]) :-
	write_class_member(Stream,ClassMember),
	write_class_members(Stream,ClassMembers).



write_class_member(Stream,eol_comment(Comment)) :- !,
	write(Stream,'// '),write(Stream,Comment),nl(Stream).
write_class_member(Stream,constructor_decl(Functor/Arity,ParamDecls,Stmts)) :- !,
	write(Stream,'public '),write(Stream,Functor),write(Stream,Arity),
	write(Stream,'('),
	write_param_decls(Stream,ParamDecls),
	write(Stream,'){\n'),
	write_stmts(Stream,Stmts),
	write(Stream,'}\n').
write_class_member(Stream,method_decl(Visibility,ReturnType,Identifier,ParamDecls,Stmts)) :- !,
	write(Stream,Visibility),write(Stream,' '),
	write_type(Stream,ReturnType),write(Stream,' '),
	write(Stream,Identifier),
	write(Stream,'('),
	write_param_decls(Stream,ParamDecls),
	write(Stream,'){\n'),
	write_stmts(Stream,Stmts),
	write(Stream,'}\n').
write_class_member(Stream,field_decl(goal_stack)) :- !,
	write(Stream,'private GoalStack goalStack = GoalStack.EMPTY_GOAL_STACK;\n').
write_class_member(Stream,field_decl(Modifiers,Type,Name,Expression)) :- !,
	write_field_decl_modifiers(Stream,Modifiers),
	write(Stream,'private '),
	write_type(Stream,Type),write(Stream,' '),write(Stream,Name),
	( 	Expression == int(0) ->
		write(Stream,'/* = 0; */')
	;	
		write(Stream,' = '),
		write_expr(Stream,Expression)
	),
	write(Stream,';\n').		
write_class_member(Stream,field_decl(Modifiers,Type,Name)) :- !,
	write_field_decl_modifiers(Stream,Modifiers),
	write(Stream,'private '),
	write_type(Stream,Type),write(Stream,' '),write(Stream,Name),
	write(Stream,';\n').
write_class_member(_Stream,Member) :- throw(internal_error(write_class_member/2,['unknown member:',Member])).



write_field_decl_modifiers(_Stream,[]).
write_field_decl_modifiers(Stream,[Modifier|Modifiers]) :-
	write_field_decl_modifier(Stream,Modifier),
	write(Stream,' '),
	write_field_decl_modifiers(Stream,Modifiers).



write_field_decl_modifier(Stream,final) :- write(Stream,final).



% A predicate must not have any arguments (do :- ...)
write_param_decls(_Stream,[]).
write_param_decls(Stream,[ParamDecl|ParamDecls]) :-
	write_param_decl(Stream,ParamDecl),
	write_further_param_decls(Stream,ParamDecls).



write_further_param_decls(_Stream,[]).
write_further_param_decls(Stream,[ParamDecl|ParamDecls]) :- 
	write(Stream,', '),
	write_param_decl(Stream,ParamDecl),
	write_further_param_decls(Stream,ParamDecls).



write_param_decl(Stream,param_decl(Type,Name)) :-
	write(Stream,'final '),
	write_type(Stream,Type),
	write(Stream,' '),
	write(Stream,Name).



write_type(Stream,type(int)) :- !,write(Stream,'int').
write_type(Stream,type(boolean)) :- !,write(Stream,'boolean').
write_type(Stream,type(void)) :- !,write(Stream,'void').
write_type(Stream,type(goal)) :- !,write(Stream,'Goal').
write_type(Stream,type(term)) :- !,write(Stream,'Term').
write_type(Stream,type(state)) :- !,write(Stream,'State').
write_type(Stream,type(variable)) :- !,write(Stream,'Variable').
write_type(Stream,type(complex_term)) :- !,write(Stream,'CompoundTerm').
write_type(Stream,type(atomic(string_atom))) :- !,write(Stream,'StringAtom').
write_type(Stream,type(atomic(int_value))) :- !,write(Stream,'IntValue').
write_type(Stream,type(atomic(float_value))) :- !,write(Stream,'FloatValue').
write_type(_Stream,Type) :- throw(internal_error(write_type/2,['unknown type:',Type])).



write_stmts(_Stream,[]).
write_stmts(Stream,[Stmt|Stmts]) :-
	write_stmt(Stream,Stmt),
	write_stmts(Stream,Stmts).



write_stmt(_Stream,empty) :- !.
write_stmt(Stream,eol_comment(Comment)) :- !,
	write(Stream,'// '),write(Stream,Comment),nl(Stream).
write_stmt(Stream,expression_statement(Expression)) :- !,
	write_expr(Stream,Expression),
	write(Stream,';\n').
write_stmt(Stream,forever(LoopName,Stmts)) :- !,
	write(Stream,LoopName),write(Stream,': do{\n'),
	write_stmts(Stream,Stmts),
	write(Stream,'} while(true);\n').	
write_stmt(Stream,continue(LoopName)) :- !,
	write(Stream,'continue '),write(Stream,LoopName),write(Stream,';\n').
write_stmt(Stream,return(Expression)) :- !,
	write(Stream,'return '),
	write_expr(Stream,Expression),
	write(Stream,';\n').
write_stmt(Stream,if(ConditionExpression,TrueStmts)) :- !,
	write(Stream,'if('),write_expr(Stream,ConditionExpression),write(Stream,'){\n'),
	write_stmts(Stream,TrueStmts),
	write(Stream,'}\n').	
write_stmt(Stream,if(ConditionExpression,TrueStmts,FalseStmts)) :- !,
	write(Stream,'if('),write_expr(Stream,ConditionExpression),write(Stream,'){\n'),
	write_stmts(Stream,TrueStmts),
	write(Stream,'} else {\n'),
	write_stmts(Stream,FalseStmts),
	write(Stream,'}\n').
write_stmt(Stream,local_variable_decl(Type,Name,Expression)) :- !,
	write_type(Stream,Type),write(Stream,' '),write(Stream,Name),
	write(Stream,' = '),
	write_expr(Stream,Expression),
	write(Stream,';\n').	
write_stmt(Stream,clear_goal_stack) :- !,
	write(Stream,'this.goalStack = GoalStack.EMPTY_GOAL_STACK;\n').	
write_stmt(Stream,abort_pending_goals_and_clear_goal_stack) :- !,
	write(Stream,'this.goalStack = goalStack.abortPendingGoals();\n').
write_stmt(Stream,push_onto_goal_stack(GoalExpression)) :- !,
	write(Stream,'this.goalStack = goalStack.put('),
	write_expr(Stream,GoalExpression),
	write(Stream,');\n').
write_stmt(Stream,abort_and_remove_top_level_goal_from_goal_stack) :- !,
	write(Stream,'this.goalStack = goalStack.abortTopLevelGoal();\n').	
write_stmt(Stream,remove_top_level_goal_from_goal_stack) :- !,
	write(Stream,'this.goalStack = goalStack.drop();\n').
write_stmt(Stream,switch(top_level,Expression,Cases)) :- !,
	write(Stream,'switch('),write_expr(Stream,Expression),write(Stream,'){\n'),
	write_cases(Stream,Cases),
	write(Stream,'default:\n'),
	write(Stream,'// should never be reached\n'),
	write(Stream,'throw new Error("internal compiler error");\n'),
	write(Stream,'}\n').
write_stmt(Stream,switch(inside_forever,Expression,Cases)) :- !,
	write(Stream,'switch('),write_expr(Stream,Expression),write(Stream,'){\n'),
	write_cases(Stream,Cases),
	write(Stream,'}\n').
write_stmt(Stream,manifest_state(ReceiverExpr,LValue)) :- !,
	write_lvalue(Stream,LValue),
	write(Stream,' = ('),
	write_expr(Stream,ReceiverExpr),
	write(Stream,').manifestState();\n').
write_stmt(Stream,reincarnate_state(ReceiverExpr)) :- !,
	write(Stream,'if ('),
	write_expr(Stream,ReceiverExpr),
	write(Stream,' != null)'),
	write_expr(Stream,ReceiverExpr),
	write(Stream,'.reincarnate();\n').
write_stmt(Stream,create_undo_goal_and_put_on_goal_stack([TermExpression|TermExpressions])) :- !,
	write(Stream,'this.goalStack = goalStack.put(UndoGoal.create ('),
	write_expr(Stream,TermExpression),
	write_complex_term_args(Stream,TermExpressions),
	write(Stream,'));\n').
write_stmt(_Stream,Stmt) :- throw(internal_error(write_stmt/2,['unknown statement ',Stmt])).


write_cases(_Stream,[]).
write_cases(Stream,[case(ConstantExpression,Stmts)|Cases]) :-
	write(Stream,'case '),write_expr(Stream,ConstantExpression),write(Stream,':{\n'),
	write_stmts(Stream,Stmts),
	write(Stream,'}\n'),
	write_cases(Stream,Cases).
	
	

write_lvalue(Stream,field_ref(ReceiverExpression,Identifer)) :-
	write_expr(Stream,ReceiverExpression),
	write(Stream,'.'),
	write(Stream,Identifer).
write_lvalue(Stream,local_variable_ref(Identifer)) :-
	write(Stream,Identifer).



write_exprs(_Stream,[]).
write_exprs(Stream,[Expression|Expressions]) :-
	write_expr(Stream,Expression),
	write_further_exprs(Stream,Expressions).



write_further_exprs(_Stream,[]).
write_further_exprs(Stream,[Expression|Expressions]) :-
	write(Stream,', '),
	write_expr(Stream,Expression),
	write_further_exprs(Stream,Expressions).



write_expr(Stream,unify(ATermExpression,BTermExpression)) :- !,
	write(Stream,'('),
	write_expr(Stream,ATermExpression),
	write(Stream,').unify('),
	write_expr(Stream,BTermExpression),
	write(Stream,')').
write_expr(Stream,arithmetic_evluation(ArithTerm)) :- !,
	write_expr(Stream,eval_term(ArithTerm)).
write_expr(Stream,arithmetic_comparison(Operator,LArithTerm,RArithTerm)) :- !,
	map_arith_comp_operator_to_java_operator(Operator,JavaOperator),
	write_expr(Stream,eval_term(LArithTerm)),
	write(Stream,' '),write(Stream,JavaOperator),write(Stream,' '),
	write_expr(Stream,eval_term(RArithTerm)).
write_expr(Stream,eval_term(ArithTerm)) :- !,
	write_arith_term(Stream,ArithTerm).
write_expr(Stream,assignment(LValue,Expression)) :- !,
	write_lvalue(Stream,LValue),
	write(Stream,' = '),
	write_expr(Stream,Expression).
write_expr(Stream,reference_comparison(LExpression,RExpression)) :- !,
	write_lvalue(Stream,LExpression),
	write(Stream,' == '),
	write_expr(Stream,RExpression).	
write_expr(Stream,field_ref(ReceiverExpression,Identifer)) :- !, 
	write_expr(Stream,ReceiverExpression),
	write(Stream,'.'),
	write(Stream,Identifer).
write_expr(Stream,local_variable_ref(Identifer)) :- !,
	write(Stream,Identifer).
write_expr(Stream,boolean(Value)) :- !,
	write(Stream,Value).
write_expr(Stream,int(Value)) :- !,
	write(Stream,Value).
write_expr(Stream,null) :- !,
	write(Stream,'null').
write_expr(Stream,self) :- !,
	write(Stream,'this').	
write_expr(Stream,string(Value)) :- !,
	write(Stream,'"'),
	write(Stream,Value),
	write(Stream,'"').
write_expr(Stream,not(BooleanExpression)) :- !,
	write(Stream,'!'),
	write_expr(Stream,BooleanExpression).
write_expr(Stream,method_call(ReceiverExpression,Identifier,Expressions)) :- !,
	write_expr(Stream,ReceiverExpression),
	write(Stream,'.'),
	write(Stream,Identifier),
	write(Stream,'('),
	write_exprs(Stream,Expressions),
	write(Stream,')').
write_expr(Stream,local_variable_ref(Identifier)):- !,
	write(Stream,Identifier).
write_expr(Stream,static_predicate_call(string_atom(Functor))):- !,
	map_functor_to_class_name(Functor/0,ClassName),
	write(Stream,'new '),write(Stream,ClassName),write(Stream,'0()').
write_expr(Stream,static_predicate_call(complex_term(string_atom(Functor),ArgsExpressions))):- !,
	length(ArgsExpressions,ArgsCount),
	ArgsExpressions = [FirstArgExpression|MoreArgsExpressions],
	length(ArgsExpressions,Arity),
	map_functor_to_class_name(Functor/Arity,ClassName),
	write(Stream,'new '),write(Stream,ClassName),write(Stream,ArgsCount),write(Stream,'('),
	write_expr(Stream,FirstArgExpression),
	write_complex_term_args(Stream,MoreArgsExpressions),
	write(Stream,')').
write_expr(Stream,call_term(TermExpression)):- !,
	write_expr(Stream,TermExpression),
	write(Stream,'.call()').	
write_expr(Stream,get_top_element_from_goal_stack) :- !,
	write(Stream,'this.goalStack.peek()').
/* E X P R E S S I O N S   W I T H   T Y P E   T E R M  */
write_expr(Stream,clv(I)) :- !,
	write(Stream,'this.clv'),write(Stream,I).
write_expr(Stream,arg(I)) :- !,
	write(Stream,'this.arg'),write(Stream,I).	
write_expr(Stream,variable) :- !,
	write(Stream,'variable()').
write_expr(Stream,anonymous_variable) :- !, % TODO do we need to distinguish between "named" and anonymous variables?
	write(Stream,'variable()').
write_expr(Stream,int_value(-1)) :- !,
	write(Stream,'IntValue_M1').	
write_expr(Stream,int_value(0)) :- !,
		write(Stream,'IntValue_0').
write_expr(Stream,int_value(1)) :- !,
	write(Stream,'IntValue_1').	
write_expr(Stream,int_value(2)) :- !,
	write(Stream,'IntValue_2').
write_expr(Stream,int_value(3)) :- !,
	write(Stream,'IntValue_3').				
write_expr(Stream,int_value(Value)) :- !,
	write(Stream,'IntValue.get('),write(Stream,Value),write(Stream,')').
write_expr(Stream,int_value_expr(Expr)) :- !,
	write(Stream,'IntValue.get('),write_expr(Stream,Expr),write(Stream,')').
write_expr(Stream,float_value(Value)) :- !,
	write(Stream,'FloatValue.get('),write(Stream,Value),write(Stream,')').
write_expr(Stream,string_atom('=')) :- !,write(Stream,'UNIFY').
write_expr(Stream,string_atom('\\=')) :- !,write(Stream,'DOES_NOT_UNIFY').
write_expr(Stream,string_atom(',')) :- !,write(Stream,'AND').
write_expr(Stream,string_atom(';')) :- !,write(Stream,'OR').
write_expr(Stream,string_atom('!')) :- !,write(Stream,'CUT').
write_expr(Stream,string_atom('*->')) :- !,write(Stream,'SOFT_CUT').
write_expr(Stream,string_atom('->')) :- !,write(Stream,'IF_THEN').
write_expr(Stream,string_atom('true')) :- !,write(Stream,'TRUE').
write_expr(Stream,string_atom('false')) :- !,write(Stream,'FALSE').
write_expr(Stream,string_atom('fail')) :- !,write(Stream,'FAIL').
write_expr(Stream,string_atom('not')) :- !,write(Stream,'NOT').
write_expr(Stream,string_atom('\\+')) :- !,write(Stream,'NOT_OPERATOR').
write_expr(Stream,string_atom('is')) :- !,write(Stream,'IS').
write_expr(Stream,string_atom('*')) :- !,write(Stream,'MULT').
write_expr(Stream,string_atom('-')) :- !,write(Stream,'MINUS').
write_expr(Stream,string_atom('+')) :- !,write(Stream,'PLUS').
write_expr(Stream,string_atom('<')) :- !,write(Stream,'ARITH_SMALLER_THAN').
write_expr(Stream,string_atom('=<')) :- !,write(Stream,'ARITH_SMALLER_OR_EQUAL_THAN').
write_expr(Stream,string_atom('>')) :- !,write(Stream,'ARITH_LARGER_THAN').
write_expr(Stream,string_atom('>=')) :- !,write(Stream,'ARITH_LARGER_OR_EQUAL_THAN').
write_expr(Stream,string_atom('=:=')) :- !,write(Stream,'ARITH_IS_EQUAL').
write_expr(Stream,string_atom('=\\=')) :- !,
	write(Stream,'ARITH_IS_NOT_EQUAL').
write_expr(Stream,string_atom('[]')) :- !,write(Stream,'EMPTY_LIST').
write_expr(Stream,string_atom('.')) :- !,write(Stream,'LIST').	
write_expr(Stream,string_atom(Value)) :- !,
	write(Stream,'StringAtom.get("'),write(Stream,Value),write(Stream,'")').
write_expr(Stream,complex_term(string_atom(','),[LT,RT])) :- !,
	write(Stream,'and('),
	write_expr(Stream,LT),
	write(Stream,','),
	write_expr(Stream,RT),
	write(Stream,')').
write_expr(Stream,complex_term(string_atom(';'),[LT,RT])) :- !,
	write(Stream,'or('),
	write_expr(Stream,LT),
	write(Stream,','),
	write_expr(Stream,RT),
	write(Stream,')').
write_expr(Stream,complex_term(string_atom('.'),[LT,RT])) :- !,
	write(Stream,'list('),
	write_expr(Stream,LT),
	write(Stream,','),
	write_expr(Stream,RT),
	write(Stream,')').		
write_expr(Stream,complex_term(Functor,Args)) :- !,
	write(Stream,'compoundTerm('),
	write_expr(Stream,Functor),
	write_complex_term_args(Stream,Args),
	write(Stream,')').	
write_expr(_Stream,Expr) :- % the last case... 
	throw(internal_error(write_expr/2,['unknown expression ',Expr])).




% TODO rename to "write_expr_list_1_to_n"
write_complex_term_args(_Stream,[]).
write_complex_term_args(Stream,[Arg|Args]) :-
	write(Stream,', '),
	write_expr(Stream,Arg),
	write_complex_term_args(Stream,Args).



write_arith_term(Stream,int_value(X)) :- !,
	write(Stream,X),write(Stream,'l ').
write_arith_term(Stream,local_variable_ref(Id)) :- !,
	write_expr(Stream,local_variable_ref(Id)),write(Stream,'.intEval() ').
write_arith_term(Stream,field_ref(Receiver,Id)) :- !,
	write_expr(Stream,field_ref(Receiver,Id)),write(Stream,'.intEval() ').
write_arith_term(Stream,complex_term(string_atom('-'),[ArithTerm])) :- !,
	map_arith_prolog_operator_to_java_operator('-',JavaOperator),
	write(Stream,JavaOperator),
	write(Stream,'('),
	write_arith_term(Stream,ArithTerm),
	write(Stream,')').
write_arith_term(Stream,complex_term(string_atom(Op),[LArithTerm,RArithTerm])) :- !,
	map_arith_prolog_operator_to_java_operator(Op,JavaOperator),
	write(Stream,'('),
	write_arith_term(Stream,LArithTerm),
	write(Stream,JavaOperator),
	write_arith_term(Stream,RArithTerm),
	write(Stream,')').
write_arith_term(_Stream,X) :- 
	throw(programming_error(['illegal arithmetic term: ',X])).



/* ************************************************************************** *\
 *                                                                            *
 *      P R E D I C A T E   R E G I S T R A T I O N  (INFRASTRUCTURE)         *
 *                                                                            *
\* ************************************************************************** */

write_predicate_registration(Stream,Functor/Arity) :-
	call_foreach_i_in_0_to_u(Arity,array_acces('args'),ArrayAccesses),
	atomic_list_concat(ArrayAccesses,',',ConstructorArgs),
	
	write_atomic_list(
		Stream,
		[
		'/*\n',
		' * Generated by SAE Prolog - www.opal-project.de\n',
		' * \n',	
		' * DO NOT CHANGE MANUALLY - THE CLASS WILL COMPLETELY BE REGENERATED\n',			
		' */\n',
		'package predicates;\n',
		'import saere.*;\n\n',
		'public class ',Functor,Arity,'Factory extends PredicateFactoryNArgs{\n',
		'	private ',Functor,Arity,'Factory(){\n',
		'		/*NOTHING TO DO*/\n',
		'	}\n',
		'	public final static PredicateIdentifier IDENTIFIER = new PredicateIdentifier(StringAtom.get("',Functor,'"), ',Arity,');\n\n',
		'	public final static PredicateFactory INSTANCE = new ',Functor,Arity,'Factory();\n\n',
		'	public Goal createInstance(Term[] args) {\n',
		'		return new ',Functor,Arity,'(',ConstructorArgs,');\n',
		'	}\n',
		'	public static void registerWithPredicateRegistry(PredicateRegistry registry) {\n',
		'		registry.register(IDENTIFIER, INSTANCE);\n',
		'	}\n',
		'}\n'
		]
	).



array_acces(ArrayName,N,ArrayAccess) :- 
	atomic_list_concat([ArrayName,'[',N,']'],ArrayAccess).
	
	

/* ************************************************************************** *\
 *                                                                            *
 *              G E N E R A L   H E L P E R   P R E D I C A T E S             *
 *                                                                            *
\* ************************************************************************** */
map_functor_to_class_name('='/2,'Unify') :- !.
map_functor_to_class_name('\\='/2,'NotUnify') :- !.
map_functor_to_class_name(','/2,'And') :- !.
map_functor_to_class_name(';'/2,'Or') :- !.
map_functor_to_class_name('!'/0,'Cut') :- !.
map_functor_to_class_name('*->'/2,'SoftCut') :- !.
map_functor_to_class_name('->'/2,'If_Then') :- !.
map_functor_to_class_name('true'/0,'True') :- !.
map_functor_to_class_name('false'/0,'False') :- !.
map_functor_to_class_name('fail'/0,'False') :- !.
map_functor_to_class_name('not'/1,'Not') :- !.
map_functor_to_class_name('/'('\\+',1),'Not') :- !.
map_functor_to_class_name('is'/2,'Is') :- !.
map_functor_to_class_name('<'/2,'Smaller') :- !.
map_functor_to_class_name('=<'/2,'SmallerOrEqual') :- !.
map_functor_to_class_name('>'/2,'Larger') :- !.
map_functor_to_class_name('>='/2,'LargerOrEqual') :- !.
map_functor_to_class_name('=:='/2,'ArithEqual') :- !.
map_functor_to_class_name('=\\='/2,'ArithNotEqual') :- !.
% FURTHER BUILT-IN PREDICATES:
map_functor_to_class_name('atom'/1,'Atom') :- !.
map_functor_to_class_name('number'/1,'Number') :- !.
map_functor_to_class_name('append'/3,'Append') :- !.
map_functor_to_class_name('member'/2,'Member') :- !.
map_functor_to_class_name('time'/1,'Time') :- !.
map_functor_to_class_name('repeat'/0,'Repeat') :- !.
map_functor_to_class_name('ground'/1,'Ground') :- !.
map_functor_to_class_name(Functor/_Arity,Functor).



map_arith_comp_operator_to_java_operator('<','<') :- !.
map_arith_comp_operator_to_java_operator('=<','<=') :- !.
map_arith_comp_operator_to_java_operator('>','>') :- !.
map_arith_comp_operator_to_java_operator('>=','>=') :- !.
map_arith_comp_operator_to_java_operator('=:=','==') :- !.
map_arith_comp_operator_to_java_operator('=\\=','!=') :- !.
map_arith_comp_operator_to_java_operator(Op,_) :- throw(programming_error(['unsupported operator: ',Op])).


map_arith_prolog_operator_to_java_operator('/\\','&') :- !.
map_arith_prolog_operator_to_java_operator('\\/','|') :- !.
map_arith_prolog_operator_to_java_operator('>>','>>') :- !.
map_arith_prolog_operator_to_java_operator('<<','<<') :- !.
map_arith_prolog_operator_to_java_operator('+','+') :- !.
map_arith_prolog_operator_to_java_operator('-','-') :- !. % unary and binary minus
map_arith_prolog_operator_to_java_operator('*','*') :- !.
map_arith_prolog_operator_to_java_operator('//','/') :- !.
map_arith_prolog_operator_to_java_operator('mod','%') :- !.
map_arith_prolog_operator_to_java_operator(Op,_) :- throw(programming_error(['unsupported operator: ',Op])).



%map_functor_to_class_name('*').
%map_functor_to_class_name('-').
%map_functor_to_class_name('+').
%map_functor_to_class_name('[]').
%map_functor_to_class_name('.').	