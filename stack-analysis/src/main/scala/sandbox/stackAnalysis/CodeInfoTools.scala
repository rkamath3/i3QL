package sandbox.stackAnalysis

import de.tud.cs.st.bat.resolved._

/**
 * Created with IntelliJ IDEA.
 * User: Mirko
 * Date: 01.11.12
 * Time: 16:02
 * To change this template use File | Settings | File Templates.
 */
object CodeInfoTools {
  def getNextPC(a: Array[Instruction], currentPC: Int): Int = {

    val testPC = currentPC + 1
    if (testPC >= a.length || testPC <= 0)
      -1
    else if (a(testPC) == null)
      getNextPC(a, testPC)
    else
      testPC
  }

  def distinctAppend[T](a: List[T], b: List[T]): List[T] = {
    if (a == Nil)
      b
    else if (b == Nil)
      a
    else if (b.contains(a.head))
      distinctAppend(a.tail, b)
    else
      distinctAppend(a.tail, a.head :: b)
  }
}
