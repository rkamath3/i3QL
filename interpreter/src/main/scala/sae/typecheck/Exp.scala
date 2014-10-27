package sae.typecheck

import idb.syntax.iql.IR.Rep
import idb.BagTable

/**
 * Created by seba on 27/10/14.
 */
object Exp {
  type Lit = Any
  abstract class ExpKind
  type ExpKey = Int
  type ExpTuple = (ExpKey, ExpKind, Seq[Lit], Seq[ExpKey])
  def id(e: Rep[ExpTuple]) = e._1
  def kind(e: Rep[ExpTuple]) = e._2
  def lits(e: Rep[ExpTuple]) = e._3
  def subseq(e: Rep[ExpTuple]) = e._4

  private var _nextKey = 0
  private[this] var _expMap = Map[Exp, (ExpKey, Int)]()
  private def bindExp(e: Exp, k: ExpKey): Unit = {
    _expMap.get(e) match {
      case None => _expMap += e -> (k, 1)
      case Some((`k`, count)) => _expMap += e -> (k, count+1)
      case Some((k2, count)) => throw new IllegalStateException(s"conflicting key bindings $k and $k2 for expression $e")
    }
  }
  private def unbindExp(e: Exp): Unit = {
    _expMap.get(e) match {
      case None => {}
      case Some((k, 1)) => _expMap -= e
      case Some((k, count)) => _expMap += e -> (k, count-1)
    }
  }
  private def lookupExp(e: Exp) = _expMap.get(e).map(_._1)
  private def lookupExpCount(e: Exp) = _expMap.get(e).map(_._2).getOrElse(0)
  private def lookupExpWithCount(e: Exp) = _expMap.get(e)

  val table = BagTable.empty[ExpTuple]


  def nextKey() = {
    val k = _nextKey
    _nextKey += 1
    k
  }

  import scala.language.implicitConversions
  implicit def constructable(k: ExpKind) = new Constructable(k)
  class Constructable(k: ExpKind) {
    def apply(): Exp = Exp(k, scala.Seq(), scala.Seq())
    def apply(l: Lit, lits: Lit*): Exp = Exp(k, l +: scala.Seq(lits:_*), scala.Seq())
    def apply(e: Exp, sub: Exp*): Exp = Exp(k, scala.Seq(), e +: scala.Seq(sub:_*))
  }
}

import Exp._
case class Exp(kind: ExpKind, lits: Seq[Lit], sub: Seq[Exp]) {
  def key = lookupExp(this).get
  def getkey = lookupExp(this)

  def insert: ExpKey = {
    val subkeys = sub map (_.insert)
    val key = getkey.getOrElse(nextKey())
    println(s"insert ${(key, kind, lits, subkeys)}*${lookupExpCount(this)}")
    table += ((key, kind, lits, subkeys))
    bindExp(this, key)
    println(s"inserted ${(key, kind, lits, subkeys)}*${lookupExpCount(this)}")
    key
  }

  def remove: ExpKey = {
    val subkeys = sub map (_.remove)
    val k = key
    println(s"remove ${(k, kind, lits, subkeys)}*${lookupExpCount(this)}")
    table -= ((k, kind, lits, subkeys))
    unbindExp(this)
    println(s"removed ${(k, kind, lits, subkeys)}*${lookupExpCount(this)}")
    k
  }

  def updateExp(old: Exp, e: Exp, oldsubkeys: Seq[ExpKey], newsubkeys: Seq[ExpKey]): ExpKey = {
    lookupExpWithCount(old) match {
      case Some((oldkey, 1)) =>
        println(s"update-old ($oldkey, $kind, $lits, $oldsubkeys)*${lookupExpCount(this)} -> ($oldkey, ${e.kind}, ${e.lits}, $newsubkeys)*${lookupExpCount(e)}")
        if (oldsubkeys != newsubkeys)
          table ~= (oldkey, old.kind, old.lits, oldsubkeys) -> (oldkey, e.kind, e.lits, newsubkeys)
        unbindExp(old)
        bindExp(e, oldkey)
        println(s"updated-old ($oldkey, $kind, $lits, $oldsubkeys)*${lookupExpCount(this)} -> ($oldkey, ${e.kind}, ${e.lits}, $newsubkeys)*${lookupExpCount(e)}")
        oldkey
      case Some((oldkey, _)) =>
        val newkey = nextKey()
        println(s"update-new ($oldkey, $kind, $lits, $oldsubkeys)*${lookupExpCount(this)} -> ($newkey, ${e.kind}, ${e.lits}, $newsubkeys)*${lookupExpCount(e)}")
        table ~= (oldkey, old.kind, old.lits, oldsubkeys) -> (newkey, e.kind, e.lits, newsubkeys)
        unbindExp(old)
        bindExp(e, newkey)
        println(s"updated-new ($oldkey, $kind, $lits, $oldsubkeys)*${lookupExpCount(this)} -> ($newkey, ${e.kind}, ${e.lits}, $newsubkeys)*${lookupExpCount(e)}")
        newkey
      case None => throw new IllegalStateException(s"Cannot update unbound expression $old to $e")
    }
  }

  def replaceWith(e: Exp): ExpKey = {
//    println(s"replace $this -> $e")
    if (kind == e.kind && lits == e.lits && sub.length == e.sub.length) {
      if (sub == e.sub)
        key
      else {
        val oldkey = key
        val oldsubkeys = sub map (_.key)
        val newsubkeys = sub.zip(e.sub).map(p => p._1.replaceWith(p._2))
        updateExp(this, e, oldsubkeys, newsubkeys)
      }
    }
    else {
      val oldsubkeys = sub map (_.remove)
      val newsubkeys = e.sub map (_.insert) // will be shared with previous subtrees due to _expMap
      updateExp(this, e, oldsubkeys, newsubkeys)
    }
  }

  override def toString = {
    val subs = lits.map(_.toString) ++ sub.map(_.toString)
    val subssep = subs.flatMap(s => Seq(", ", s)).tail
    val substring = subssep.foldLeft("")(_+_)
    val keyString = getkey match {case None => ""; case Some(k) => s"$k@"}
    s"$keyString$kind($substring)"
  }
}
