package saere.database.index;

import java.util.Iterator;

import saere.Term;

/**
 * A {@link TrieBuilder} for <i>Complex Term Insertion</i> (CTI). 
 * {@link Trie}s created with this {@link TrieBuilder} have as much 
 * {@link Trie} nodes as required, but not more. However, the insertion process 
 * is a bit more <i>complex</i> (and may take more time).
 * 
 * @author David Sullivan
 * @version 0.6, 10/18/2010
 */
public class ComplexTrieBuilder extends TrieBuilder {
	
	public ComplexTrieBuilder(TermFlattener flattener, int mapThreshold) {
		super(flattener, mapThreshold);
	}
	
	@Override
	public Trie insert(Term term, Trie start) {
		current = start;
		stack = flattener.flatten(term);
		Trie insertionNode = null; // the trie node where the specified term will be added
		int match;
		while (insertionNode == null) {
			
			if (current.getParent() == null) { // root 
				
				if (current.getFirstChild() == null) { // create the very first node and add term directly
					current.setFirstChild(new MultiStorageLeaf(current, makeComplexLabel(), term));
					insertionNode = current.getFirstChild();
				} else { // move to child
					current = current.getFirstChild(); // set current for next insert()
				}

			} else { // !root
				
				// Check wether we can have full match
				Trie fullMatch = getChild(current, stack.peek());
				if (fullMatch != null) {
					if (fullMatch.getLabel().length() < stack.size()) {
						
						// insert as child
						stack.pop(fullMatch.getLabel().length());
						if (current.getFirstChild() == null) { // create first child and add term directly
							current.setFirstChild(new MultiStorageLeaf(current, makeComplexLabel(), term));
							insertionNode = current.getFirstChild();
						} else { // move to child
							current = current.getFirstChild();
						}
						
					} else { // match == stack.size(), insert here...
						
						if (current.isSingleStorageLeaf()) { // Already a storing node
							addTerm(current, term);
						} else {
							MultiStorageLeaf storageTrie = new MultiStorageLeaf(current.getParent(), makeComplexLabel(), term);
							replace(current, storageTrie);
							current = storageTrie;
						}
						
						insertionNode = current;
					}
				} else {
					// Check wether we have a partial match or none at all
					match = match();
					if (match > 0) {
						
						// split...
						Label newLabel = current.getLabel().split(match - 1);
						Trie mediator;
						TermList termList = current.getTerms();
						current.setTerms(null);
						
						if (current.isHashNode()) {
							mediator = new InnerHashNode(current, newLabel, current.getLastChild());
						} else {
							mediator = new MultiStorageLeaf(current, newLabel, null);
						}
						mediator.setTerms(termList);
						
						// insert mediator
						if (current.getFirstChild() != null) {
							mediator.setFirstChild(current.getFirstChild());
							
							if (current.isHashNode()) {
								mediator.setMap(current.getMap());
							}
							
							// set mediator as parent for all children
							Trie child = mediator.getFirstChild();
							while (child != null) {
								child.setParent(mediator);
								child = child.getNextSibling();
							}
						}
						
						//current.setFirstChild(mediator);
						if (current.isHashNode()) {
							MultiStorageLeaf newCurrent = new MultiStorageLeaf(current.getParent(), current.getLabel(), null);
							replace(current, newCurrent);
							current = newCurrent;
							newCurrent.setFirstChild(mediator);
						}
						
						// and go on...
						stack.pop(match);
						current = mediator;
						
					} else { // no match
						
						// Check wether we can have a full match
						if (current.getParent().getFirstChild() == current) {
							Trie searched = getChild(current, stack.peek());
							if (searched != null) {
								
							}
						}
						
						if (current.getNextSibling() == null) { // create first next sibling and add term directly
							current.setNextSibling(new MultiStorageLeaf(current.getParent(),makeComplexLabel(), term));
							insertionNode = current.getNextSibling();
						} else { // move to next sibling
							current = current.getNextSibling();
						}
					}
					
				}
			}
			
		}
		
		return insertionNode;
	}

	@Override
	public void remove(Term term, Trie start) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Iterator<Term> iterator(Trie start, Term query) {
		if (flattener instanceof ShallowFlattener) {
			//return new ShallowComplexQueryIterator(this, start, flattener.flatten(query));
			throw new UnsupportedOperationException("Not yet implemented");
		} else if (flattener instanceof FullFlattener) {
			throw new UnsupportedOperationException("Not implemented");
		} else {
			throw new UnsupportedOperationException("Unexpected term flattener");
		}
	}
	
	@Override
	public String toString() {
		return flattener.toString() + "-complex";
	}
	
	private int match() {
		Label[] currentLabels = current.getLabel().labels();
		Label[] stackLabels = stack.array();
		int offset = stack.position();
		int min = Math.min(currentLabels.length, stack.size());
		int i;
		for (i = 0; i < min; i++) {
			if (!currentLabels[i].sameAs(stackLabels[i + offset]))
				break;
		}
		
		return i;
	}
	
	/**
	 * Creates a complex label with the current stack state.
	 * 
	 * @return A complex label based on the current stack state.
	 */
	private ComplexLabel makeComplexLabel() {
		Label[] stackLabels = stack.array();
		int offset = stack.position();
		Label[] labels = new Label[stack.size()];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = stackLabels[i + offset];
		}
		return ComplexLabel.ComplexLabel(labels);
	}
	
	private static Trie getChildWithPrefix(Trie parent, Label prefix) {
		Trie child = parent.getFirstChild();
		while (child != null) {
			if (prefix.sameAs(child.getLabel().labels()[0])) {
				return child;
			}
		}
		
		return null;
	}
	
	private void splitCurrent() {
		
	}
}
