package ivm.expressiontree

import org.scalatest.junit.{ShouldMatchersForJUnit, JUnitSuite}
import org.junit.Test
import Lifting._

/**
 * User: pgiarrusso
 * Date: 12/1/2012
 */

class OptionLiftingTests /*extends JUnitSuite with ShouldMatchersForJUnit*/ {
  // Test that this code compiles, i.e. that OptionOps.flatMap can also accept Exp[T] => Exp[Option[U]] and produce
  // Exp[Option[U]] in that case.
  asExp(Some(1)) flatMap (i => Let(i)) orElse Some(2)
}
