package sae.analyses.findbugs.stack.structure

/**
 * Created with IntelliJ IDEA.
 * User: Mirko
 * Date: 04.11.12
 * Time: 20:02
 *
 * @author Mirko
 * @author Ralf Mitschke
 *
 */
case class State(s: Stacks, l: LocVariables)
{

    def combineWith(other: State): State = {
        State (s.combineWith (other.s), l.combineWith (other.l))
    }

}

object State
{
    def createEmptyState(maxStack: Int, maxLoc: Int): State =
        State (Stacks (maxStack, Nil), LocVariables (Array.ofDim[Item](maxLoc)))

    def createStartState(maxStack: Int, maxLoc: Int): State =
        State (Stacks (maxStack, Nil).addStack (), LocVariables (Array.ofDim[Item](maxLoc)))
}
