package sandbox

import stackAnalysis._
import sae.bytecode.bat.BATDatabaseFactory
import java.io.FileInputStream
import sae.Relation
import sae.syntax.sql._
import stackAnalysis.CodeInfoTransformer
import sae.bytecode.structure.CodeInfo
import stackAnalysis.StackAnalysis

/**
 * Main class
 *
 * Created with IntelliJ IDEA.
 * User: mirko
 * Date: 22.10.12
 * Time: 15:59
 * To change this template use File | Settings | File Templates.
 */
object Main {

  def main(args: Array[String]) {

    val database = BATDatabaseFactory.create()
    val infos: Relation[CodeInfo] = compile(SELECT(*) FROM database.code)

    /*val methods: Relation[MethodDeclaration] = compile(SELECT(*) FROM database.methodDeclarations)
    val attr: Relation[CodeAttribute] = compile(SELECT(*) FROM database.codeAttributes) // WHERE (((_: CodeAttribute).declaringMethod.name) === "main"))
    val instr: Relation[InstructionInfo] = compile(SELECT(*) FROM database.instructions) // WHERE (((_: InstructionInfo).declaringMethod.name) === "main"))
    //    val query: QueryResult[(Int, Array[Int])] = compile(SELECT((codeAttribute: CodeAttribute) => (1, Array.ofDim[Int](codeAttribute.max_locals))) FROM database.codeAttributes)
    */
    val cfg = new CodeInfoCFG(infos)
    val funs = new CodeInfoTransformer(infos)
    val analysis = new StackAnalysis(infos, cfg, funs)

    database.addClassFile(new FileInputStream("stack-analysis\\resources\\Test"))

    //println(analysis.analysisResult.asList.mkString("Result: ", ", ",""))


    // println(new CodeInfoCFG(infos).computePredecessors)


    // val x: Relation[(CodeAttribute, InstructionInfo)] = compile(SELECT(*) FROM (attr, instr) WHERE (((_:InstructionInfo).declaringMethod) === (_: CodeAttribute).declaringMethod))

    /* for (m <- methods) {
 val methAttr = compile(SELECT(*) FROM attr WHERE (((_: CodeAttribute).declaringMethod) === m))
 val methInstr = compile(SELECT(*) FROM instr WHERE (((_: InstructionInfo).declaringMethod) === m))

 val cfg: CodeInfoCFG = new CodeInfoCFG(methInstr.asList.sortWith((a, b) => a.sequenceIndex < b.sequenceIndex))
 val transformers: List[(Int, AnalysisResult)] = new StackAnalysis(cfg, methAttr.asList(0).max_stack, methAttr.asList(0).max_locals).execute().reverse

 println("Result for method '" + m.name + "'")
 println(transformers.mkString("\n"))
 println()

}     */


  }

  /*
def transformers(codeInfo : Relation[CodeInfo]) : Relation[(MethodDeclaration, Array[List[Int]])] = SELECT((c: CodeInfo) => (c.declaringMethod, computePredecessorsPriv(c.code.instructions))) FROM codeInfo

private def computePredecessorsPriv(a: Array[Instruction]): Array[List[Int]] = {

val res = Array.fill[List[Int]](a.length)(null)
res(0) = Nil

var currentPC = 0
var nextPC = 0

while (nextPC != -1) {

nextPC = CodeInfoTools.getNextPC(a, currentPC)

if (nextPC != -1) {
if (a(currentPC).isInstanceOf[ConditionalBranchInstruction]) {
addToArray(res,nextPC,currentPC)
addToArray(res,currentPC + a(currentPC).asInstanceOf[ConditionalBranchInstruction].branchoffset,currentPC)
} else if (a(currentPC).isInstanceOf[UnconditionalBranchInstruction]) {
addToArray(res,currentPC + a(currentPC).asInstanceOf[UnconditionalBranchInstruction].branchoffset,currentPC)
} else {
addToArray(res,nextPC,currentPC)
}
}

currentPC = nextPC
}

println(res.mkString("CFGRes: ", ", ", ""))
return res
}

private def addToArray(a : Array[List[Int]], index : Int, add : Int ) {
if(a(index) == null)
a(index) = Nil
a(index) = add :: a(index)
}                                    */

}
