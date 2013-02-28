/* License (BSD Style License):
 *  Copyright (c) 2009, 2011
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package sae.operators.impl

import util.TransactionKeyValueObserver
import sae.{Observer, Relation}
import collection.mutable
import sae.deltas.{Update, Deletion, Addition}

/**
 *
 *
 * @author Ralf Mitschke
 */
class TransactionalAnchorAndFixPointRecursionView[Domain, Range, Key](val anchors: Relation[Range],
                                                                      val source: Relation[Domain],
                                                                      val domainKeyFunction: Domain => Key,
                                                                      val rangeKeyFunction: Range => Key,
                                                                      val step: (Domain, Range) => Range)
    extends Relation[Range]
    with TransactionKeyValueObserver[Key, Domain]
{

    anchors.addObserver (AnchorObserver)

    source addObserver this


    var additionAnchors: List[Range] = Nil
    var additionResults              = mutable.HashSet.empty[Range]
    var deletionAnchors: List[Range] = Nil
    var deletionResults              = mutable.HashSet.empty[Range]


    def keyFunc = domainKeyFunction


    private var recursionStack: List[List[Range]] = Nil

    def doRecursionForAddedElements() {
        val stackBase =
            for (anchor <- additionAnchors
                 if !additionResults.contains (anchor)
            ) yield
            {
                additionResults.add (anchor)
                element_added (anchor)
                anchor
            }

        if (stackBase.isEmpty) {
            return
        }

        recursionStack = List (stackBase)

        while (!recursionStack.isEmpty) {

            //println(recursionStack.size)
            println(recursionStack.size)
            println(additionResults.size)
            // we have derived base and now we want to derive further values recursively
            val currentResult = recursionStack.head.head
            // remove the current value from the current level
            recursionStack = recursionStack.head.tail :: recursionStack.tail


            var it: java.util.Iterator[Domain] = additions.get (rangeKeyFunction (currentResult)).iterator ()
            var nextResults: List[Range] = Nil
            while (it.hasNext) {
                val joinedElement: Domain = it.next ()
                val nextResult: Range = step (joinedElement, currentResult)
                if (!additionResults.contains (nextResult)) {
                    additionResults.add (nextResult)
                    // add elements of the next level
                    element_added (nextResult)
                    nextResults = nextResult :: nextResults
                }
            }

            // add a the next Results at the beginning of the next level
            recursionStack = nextResults :: recursionStack


            // we did not compute a new level, i.e., the next recursion level of values is empty
            // remove all empty levels
            while (!recursionStack.isEmpty && recursionStack.head == Nil) {
                recursionStack = recursionStack.tail
            }
        }

    }

    def doRecursionForRemovedElements() {
        // TODO compute the recursive values

        // all domain values are stored in the Multimap "deletions"

        for (anchor <- deletionAnchors) {
            deleteResult (anchor)
        }
    }

    private def deleteResult(delResult: Range) {

        //If the result has already been deleted or has been added by the added results.
        if (deletionResults.contains (delResult)) {
            return
            //Delete the result and continue deleting recursively.
        }
        else
        {
            deletionResults.add (delResult)
            element_removed (delResult)

            var it: java.util.Iterator[Domain] = deletions.get (rangeKeyFunction (delResult)).iterator ()
            while (it.hasNext) {
                val next: Domain = it.next ()
                val nextResult: Range = step (next, delResult)
                deleteResult (nextResult)
            }
        }
    }

    override def endTransaction() {
        sourcesTransactionEnded = true
        if (!anchorsTransactionEnded) {
            return
        }


        doRecursionForAddedElements ()
        doRecursionForRemovedElements ()
        clear ()
        sourcesTransactionEnded = false
        notifyEndTransaction ()

    }

    override def clear() {
        additionAnchors = Nil
        deletionAnchors = Nil
        additionResults = mutable.HashSet.empty[Range]
        deletionResults = mutable.HashSet.empty[Range]
        // TODO remove any data structures you define.
        // please store them as "var" and do,  x = new HashMap, or something
        super.clear ()
    }


    def foreach[T](f: (Range) => T) {
        /* do nothing, since this is a transactional view */
    }

    /**
     * Returns true if there is some intermediary storage, i.e., foreach is guaranteed to return a set of values.
     */
    def isStored = false

    def isSet = false

    var anchorsTransactionEnded = false

    var sourcesTransactionEnded = false

    object AnchorObserver extends Observer[Range]
    {


        def added(v: Range) {
            additionAnchors = v :: additionAnchors
        }

        def removed(v: Range) {
            deletionAnchors = v :: deletionAnchors
        }

        override def endTransaction() {
            anchorsTransactionEnded = true
            if (!sourcesTransactionEnded) {
                return
            }

            doRecursionForRemovedElements ()
            doRecursionForAddedElements ()
            clear ()

            anchorsTransactionEnded = false

            notifyEndTransaction ()
        }

        @deprecated
        def updated(oldV: Range, newV: Range) {
            throw new UnsupportedOperationException
        }

        def updated[U <: Range](update: Update[U]) {
            throw new UnsupportedOperationException
        }

        def modified[U <: Range](additions: Set[Addition[U]], deletions: Set[Deletion[U]], updates: Set[Update[U]]) {
            throw new UnsupportedOperationException
        }
    }

}