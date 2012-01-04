package unisson.model.kinds

/**
 *
 * Author: Ralf Mitschke
 * Created: 12.09.11 09:52
 *
 */

trait DependencyKind
        extends KindExpr
{
    def designator: String

    def asVespucciString = designator

    override def toString = designator
}