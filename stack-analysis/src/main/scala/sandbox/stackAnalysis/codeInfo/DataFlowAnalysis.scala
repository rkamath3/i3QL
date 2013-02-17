package sandbox.stackAnalysis.codeInfo

import sae.Relation
import sae.bytecode.structure.{MethodDeclaration, CodeInfo}
import sae.bytecode.BytecodeDatabase
import sae.operators.Combinable
import sae.operators.impl.TransactionalEquiJoinView
import sandbox.stackAnalysis.ResultTransformer



/**
 * Abstract class for dataflow analysis.
 *
 * Created with IntelliJ IDEA.
 * User: Mirko
 * Date: 25.10.12
 * Time: 15:10
 * To change this template use File | Settings | File Templates.
 */
// rename to control flow analysis
abstract class DataFlowAnalysis[T <: Combinable[T]](cfa: ControlFlowAnalysis, t: ResultTransformer[T])(implicit m: Manifest[T]) extends (BytecodeDatabase => Relation[MethodResult[T]]) {

  val controlFlowAnalysis: ControlFlowAnalysis = cfa
  val transformer: ResultTransformer[T] = t


  /**
   * Set to true, if the results should be printed during the dataflow analysis.
   */
  var printResults = false

  def apply(bcd: BytecodeDatabase): Relation[MethodResult[T]] = {
    val cfg: Relation[MethodCFG] = controlFlowAnalysis(bcd)

    new TransactionalEquiJoinView[CodeInfo, MethodCFG, MethodResult[T], MethodDeclaration](
      bcd.code,
      cfg,
      i => i.declaringMethod,
      c => c.declaringMethod,
      (i, c) => MethodResult[T](i.declaringMethod, computeResult(i, c.cfg))
    )

    //compile(SELECT((ci: CodeInfo, cfg: MethodCFG) => MethodResult[T](ci.declaringMethod, computeResult(ci, cfg.cfg))) FROM(bcd.code, cfg) WHERE (((_: CodeInfo).declaringMethod) === ((_: MethodCFG).declaringMethod)))
  }

  def startValue(ci: CodeInfo): T

  def emptyValue(ci: CodeInfo): T

  private def computeResult(ci: CodeInfo, cfg: IndexedSeq[List[Int]]): Array[T] = {

    //The start value of the analysis.
    val sv = startValue(ci)
    val ev = emptyValue(ci)

    //Initialize the newResult array with the empty value.
    val results: Array[T] = Array.ofDim[T](cfg.length)
    //Indicator for the fixed point.
    var resultsChanged = true

    //Iterates until fixed point is reached.
    while (resultsChanged) {
      resultsChanged = false

      var pc: Int = 0
      //Iterates over all program counters.
      while (pc < cfg.length && pc >= 0) {

        //The predecessors for the instruction at program counter pc.
        val preds: List[Int] = cfg(pc)

        //Result for this iteration for the instruction at program counter pc.
        var result: T = sv

        //If the instruction has no predecessors, the newResult will be the start value (sv)
        if (preds.length != 0) {
          //Result = transform the results at the entry labels with their transformer then combine them for a new newResult.
          result = transformer(preds.head, ci.code.instructions(preds.head), fromArray(results, preds.head, ev))
          for (i <- 1 until preds.length) {
            result = transformer(preds(i), ci.code.instructions(preds(i)), fromArray(results, preds(i), ev)).upperBound(result)
          }
        }

        //Check if the newResult has changed. If no newResult was changed during one iteration, the fixed point has been found.
        if (!result.equals(results(pc))) {
          resultsChanged = true
        }

        //Set the new newResult in the newResult array.
        results(pc) = result
        //Set the next program counter.
        pc = ci.code.instructions(pc).indexOfNextInstruction(pc, ci.code)

      }


    }
    //Print out results.
    if (printResults) {
      println(ci.declaringMethod)
      println(cfg.mkString("CFG: ", ", ", ""))
      for (i <- 0 until results.length) {
        if (ci.code.instructions(i) != null) {
          println("\t" + results(i))
          println(i + "\t" + ci.code.instructions(i))

        }
      }
      println()
    }
    //  println()
    return results

  }

  private def fromArray(ts: IndexedSeq[T], index: Int, default: T) = {
    if (ts(index) == null)
      default
    else
      ts(index)
  }


}