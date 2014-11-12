package sae.typecheck.bottomup

import sae.typecheck.Constraint._
import sae.typecheck.Exp.ExpKind
import sae.typecheck.TypeStuff._
import sae.typecheck._

/**
 * Created by seba on 12/11/14.
 */
object Nonrelational extends TypeCheck {

  private var _nextId = 0
  def freshTVar(): TVar = {
    val v = TVar(Symbol("x$" + _nextId))
    _nextId += 1
    v
  }

  def reset() {}
  def typecheckIncremental(e: Exp): Either[Type, TError] = typecheck(e)()


  def typecheck(e: Exp): () => Either[Type, TError] = {
    _nextId = 0
    val (root, leaves) = BUExp.from[ConstraintNonrelationalSolutionData](e)

    () => {
      leaves foreach (typecheckSpine(_))

      val (t, sol, reqs) = root.Type
      if (!reqs.isEmpty)
        scala.Right(s"Unresolved context requirements $reqs, type $t, solution $sol")
      else if (sol._2.isEmpty)
        scala.Left(t)
      else
        scala.Right(s"Unresolved constraints ${sol._2}, solution $sol")
    }
  }

  def typecheckSpine(e: BUExp[ConstraintNonrelationalSolutionData]): Unit ={
    val isFirstTime = e.Type == null
    val isRoot = e.parent == null

    val t = typecheckStep(e)
    if (e.Type != t) {
      e.Type = t
      if (!isRoot && isFirstTime)
        e.parent.markKidTypeAvailable(e.pos)
      if (!isRoot && e.parent.allKidTypesAvailable)
        typecheckSpine(e.parent)
    }
  }

  def typecheckStep(e: BUExp[ConstraintNonrelationalSolutionData]): ConstraintNonrelationalSolutionData = e.kind match {
    case (Num, 0) => (TNum, (Map(), Set()), Set())
    case (op, 2) if op == Add || op == Mul =>
      val (t1, sol1, reqs1) = e.kids(0).Type
      val (t2, sol2, reqs2) = e.kids(1).Type

      val lcons = EqConstraint(TNum, t1)
      val rcons = EqConstraint(TNum, t2)
      val (mcons, mreqs) = mergeReqs(reqs1, reqs2)

      val sol12 = mergeSolution(sol1, sol2)
      val sol = extendSolution(sol12, mcons + lcons + rcons)
      (TNum, sol, mreqs)
    case (Var, 0) =>
      val x = e.lits(0).asInstanceOf[Symbol]
      val X = freshTVar()
      (X, (Map(), Set()), Set(VarRequirement(x, X)))
    case (App, 2) =>
      val (t1, sol1, reqs1) = e.kids(0).Type
      val (t2, sol2, reqs2) = e.kids(1).Type

      val X = freshTVar()
      val fcons = EqConstraint(TFun(t2, X), t1)
      val (mcons, mreqs) = mergeReqs(reqs1, reqs2)

      val sol12 = mergeSolution(sol1, sol2)
      val sol = extendSolution(sol12, mcons + fcons)

      (X.subst(sol._1), sol, mreqs)
    case (Abs, 1) =>
      val x = e.lits(0).asInstanceOf[Symbol]
      val (t, sol, reqs) = e.kids(0).Type

      val X = freshTVar()
      val ((otherReqs, xcons), time) = Util.timed {
        val (xreqs, otherReqs) = reqs.partition { case VarRequirement(`x`, _) => true; case _ => false}
        val xcons = xreqs map { case VarRequirement(_, t) => EqConstraint(X, t.subst(sol._1))}
        (otherReqs, xcons)
      }
      Constraint.computeReqsTime += time
      val fsol = extendSolution(sol, xcons)
      (TFun(X, t).subst(fsol._1), fsol, otherReqs)
    case (If0, 3) =>
      val (t1, sol1, reqs1) = e.kids(0).Type
      val (t2, sol2, reqs2) = e.kids(1).Type
      val (t3, sol3, reqs3) = e.kids(2).Type

      val (mcons12, mreqs12) = mergeReqs(reqs1, reqs2)
      val (mcons23, mreqs123) = mergeReqs(mreqs12, reqs3)

      val cond = EqConstraint(TNum, t1)
      val body = EqConstraint(t2, t3)

      val sol123 = mergeSolution(sol1, mergeSolution(sol2, sol3))
      val sol = extendSolution(sol123, mcons12 ++ mcons23 + cond + body)

      (t2.subst(sol._1), sol, mreqs123)

    case (Fix, 1) =>
      val (t, sol, reqs) = e.kids(0).Type
      val X = freshTVar()
      val fixCons = EqConstraint(t, TFun(X, X))
      val fsol = extendSolution(sol, Set(fixCons))
      (X.subst(fsol._1), fsol, reqs)
  }


  def main(args: Array[String]): Unit = {
    printTypecheck(Add(Num(17), Add(Num(10), Num(2))))
    printTypecheck(Add(Num(17), Add(Num(10), Num(5))))
    printTypecheck(Abs('x, Add(Num(10), Num(5))))
    printTypecheck(Abs('x, Add(Var('x), Var('x))))
    printTypecheck(Abs('x, Add(Var('err), Var('x))))
    printTypecheck(Abs('x, Abs('y, App(Var('x), Var('y)))))
    printTypecheck(Abs('x, Abs('y, Add(Var('x), Var('y)))))
    printTypecheck(If0(Num(17), Num(0), Num(1)))

    val fac = Fix(Abs('f, Abs('n, If0(Var('n), Num(1), Mul(Var('n), App(Var('f), Add(Var('n), Num(-1))))))))
    printTypecheck("factorial", fac)
    printTypecheck("eta-expanded factorial", Abs('x, App(fac, Var('x))))

    val fib = Fix(Abs('f, Abs('n,
      If0(Var('n), Num(1),
        If0(Add(Var('n), Num(-1)), Num(1),
          Add(App(Var('f), Add(Var('n), Num(-1))),
            App(Var('f), Add(Var('n), Num(-2)))))))))
    printTypecheck("fibonacci function", fib)
    printTypecheck("factorial+fibonacci", Abs('x, Add(App(fac, Var('x)), App(fib, Var('x)))))
    printTypecheck(Abs('y, Var('y)))
  }
}


object BUExp {
  def from[T](e: Exp): (BUExp[T], Seq[BUExp[T]]) = {
    val leaves = collection.mutable.ArrayBuffer[BUExp[T]]()
    val bue = convert[T](e, null, -1, leaves)
    (bue, leaves.toSeq)
  }

  private def convert[T](e: Exp, parent: BUExp[T], pos: Int, leaves: collection.mutable.ArrayBuffer[BUExp[T]]): BUExp[T] = {
    val bue = BUExp[T](e.kind, e.lits,parent, pos)
    if (e.sub.isEmpty) {
      bue.kids = Seq()
      leaves += bue
    }
    else {
      val kids = for (i <- 0 until e.sub.size)
        yield convert[T](e.sub(i), bue, i, leaves)
      bue.kids = kids
    }
    bue
  }
}
case class BUExp[T](kind: ExpKind, lits: Seq[Any], parent: BUExp[T], pos: Int) {
  private var _kids: Seq[BUExp[T]] = _
  var availableKidTypes: Seq[Boolean] = _

  var Type: T = _

  def kids = _kids
  def kids_=(es: Seq[BUExp[T]]) = {
    _kids = es
    availableKidTypes = es map (_.Type != null)
  }

  def markKidTypeAvailable(pos: Int) =
    availableKidTypes = availableKidTypes.updated(pos, true)

  def allKidTypesAvailable = availableKidTypes.foldLeft(true)(_&&_)
}
