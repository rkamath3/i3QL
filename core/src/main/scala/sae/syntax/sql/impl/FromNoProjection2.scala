package sae.syntax.sql.impl

import sae.LazyView
import sae.syntax.sql.{STAR_KEYWORD, INLINE_WHERE_CLAUSE, FROM_CLAUSE_2}
import sae.operators.{Conversions, CrossProduct}

/**
 *
 * Author: Ralf Mitschke
 * Date: 03.08.12
 * Time: 20:08
 *
 */
private[sql] case class FromNoProjection2[DomainA <: AnyRef, DomainB <: AnyRef](relationA: LazyView[DomainA], relationB: LazyView[DomainB], distinct: Boolean)
    extends FROM_CLAUSE_2[DomainA, DomainB, (DomainA, DomainB)]
{

    def compile() =
        withDistinct (
            new CrossProduct[DomainA, DomainB](
                Conversions.lazyViewToMaterializedView (relationA),
                Conversions.lazyViewToMaterializedView (relationB)
            ),
            distinct
        )

    def WHERE(predicate: ((DomainA, DomainB)) => Boolean) =
        WhereNoProjection (
            predicate,
            new CrossProduct[DomainA, DomainB](
                Conversions.lazyViewToMaterializedView (relationA),
                Conversions.lazyViewToMaterializedView (relationB)
            ),
            distinct
        )

    def WHERE(predicatesA: INLINE_WHERE_CLAUSE[DomainA], predicatesB: INLINE_WHERE_CLAUSE[DomainB]) =
        null

    def WHERE(predicatesA: INLINE_WHERE_CLAUSE[DomainA], predicatesB: STAR_KEYWORD) = null

    def WHERE(predicatesA: STAR_KEYWORD, predicatesB: INLINE_WHERE_CLAUSE[DomainB]) = null

    def WHERE(predicateA: (DomainA) => Boolean, predicateB: (DomainB) => Boolean) = null

    def WHERE[RangeA <: AnyRef, RangeB <: AnyRef](join: ((DomainA) => RangeA, (DomainB) => RangeB)) = null

    def WHERE[DomainC <: AnyRef, RangeA <: AnyRef, RangeB <: AnyRef](join: (((DomainA, DomainB)) => RangeA, (DomainC) => RangeB)) = null

}