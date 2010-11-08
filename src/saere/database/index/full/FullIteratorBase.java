package saere.database.index.full;

import java.util.Iterator;

/**
 * Abstract base class for {@link FullTrie} iterators that contains common methods.
 * 
 * @author David Sullivan
 * @version 0.2, 9/30/2010
 */
public abstract class FullIteratorBase {
	
	/**
	 * The node from where the iteration begins. From the point of view of the
	 * iterator it works as root. Only the <tt>start</tt> node and its descendants
	 * are considered to be iterated.
	 */
	protected FullTrie start;
	
	/**
	 * The current position.
	 */
	protected FullTrie current;
	
	/**
	 * Creates a new {@link FullIteratorBase} and <b>does not</b> set the <tt>current</tt> 
	 * and <tt>start</tt> fields. This constructor is to be used if an extending 
	 * class wants to initialize the {@link #start} and {@link #current} fields itself.
	 */
	protected FullIteratorBase() {
		start = current = null;
	}
	
	/**
	 * Creates a new {@link FullIteratorBase} and sets the <tt>current</tt> 
	 * and <tt>start</tt> fields.
	 * 
	 * @param start The start node for the iteration.
	 */
	protected FullIteratorBase(FullTrie start) {
		this.start = start;
		current = start;
	}
	
	/**
	 * Finds the next <tt>next</tt>.
	 */
	protected abstract void findNext();
	
	/**
	 * Moves the current position to the next node. The next node is determined 
	 * by a depth-first search from the left to the right. If no node is left, 
	 * <tt>null</tt> is returned.
	 */
	protected void nextNode() {
		assert current != null && start != null : "current or start is/are null";
		
		if (current.firstChild != null) {
			goDown();
		} else {
			goRight();
		}
	}
	
	public void goRight() {
		while (current != null && current.nextSibling == null && current != start) { // don't go higher up than start
			goUp();
		}
		
		if (current == null)
			return;
		
		if (current != start) {
			current = current.nextSibling; // go (directly) right
		} else { // current == start
			current = null; // we treat start as root (and a root has no siblings), me march into the void
		}
	}
	
	public void goUp() {
		current = current.parent;
	}
	
	public void goDown() {
		current = current.firstChild;
	}
	
	/**
	 * Resets this iterator with the specified {@link FullTrie} as new start node. 
	 * This method can be used to avoid creating a new {@link FullIteratorBase} 
	 * object.<br/>
	 * <br/>
	 * Obviously, this method should only be used if the iterator has finished, 
	 * i.e., if a call to {@link Iterator#hasNext()} will return <tt>false</tt>.
	 * 
	 * @param newStart The new start node from where the iteration begins.
	 */
	protected void resetTo(FullTrie newStart) {
		start = newStart;
		current = start;
	}
}