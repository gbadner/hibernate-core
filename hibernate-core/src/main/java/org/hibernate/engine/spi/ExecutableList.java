/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.action.spi.Executable;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * Specialized encapsulating of the state pertaining to each Executable list.
 * <p/>
 * Manages sorting the executables (lazily)
 * <p/>
 * Manages the querySpaces affected by the executables in the list, and caches this too.
 *
 * @author Steve Ebersole
 * @author Anton Marsden
 *
 * @param <E> Intersection type describing Executable implementations
 */
@SuppressWarnings("rawtypes")
public class ExecutableList<E extends Executable & Comparable & Serializable> implements Serializable, Iterable<E>, Externalizable {

	public static final int INIT_QUEUE_LIST_SIZE = 5;

	/**
	 * Provides a sorting interface for ExecutableList.
	 * 
	 * @param <E>
	 */
	public static interface Sorter<E extends Executable> {

		/**
		 * Sorts the list.
		 */
		void sort(List<E> l);
	}

	private final ArrayList<E> executables;

	private final SortSupport<E> sortSupport;

	/**
	 * Used to hold the query spaces (table names, roughly) that all the Executable instances contained
	 * in this list define.  This information is ultimately used to invalidate cache regions as it is
	 * exposed from {@link #getQuerySpaces}.  This value being {@code null} indicates that the
	 * query spaces should be calculated.
	 */
	private transient Set<Serializable> querySpaces;

	/**
	 * Creates a new ExecutableList with the default capacity. Use of this constructor
	 * assumes that the {@link Executable} objects do not need to be sorted.
	 */
	public ExecutableList() {
		this( INIT_QUEUE_LIST_SIZE, false );
	}

	/**
	 * Creates a new ExecutableList with the default capacity.
	 *
	 * @param requiresSorting true indicates a default (natural ordering) sorter for sorting
	 *                       {@link Executable} objects; false indicates that the
	 *                       {@link Executable} objects do not need to be sorted.
	 */
	public ExecutableList(boolean requiresSorting) {
		this( INIT_QUEUE_LIST_SIZE, requiresSorting );
	}

	/**
	 * Creates a new ExecutableList with the specified {@code initialCapacity}. Use of this
	 * constructor assumes that the {@link Executable} objects do not need to be sorted.
	 *
	 * @param initialCapacity The initial capacity for instantiating the internal List
	 */
	public ExecutableList(int initialCapacity) {
		this( initialCapacity, false );
	}

	/**
	 * Creates a new ExecutableList using the specified Sorter. If {@code sorter} is
	 * {@code null}, then it is assumed that the {@link Executable} objects
	 * do not need to be sorted.
	 *
	 * @param sorter The Sorter to use; may be {@code null}
	 */
	public ExecutableList(ExecutableList.Sorter<E> sorter) {
		this( INIT_QUEUE_LIST_SIZE, sorter );
	}

	/**
	 * Creates a new ExecutableList with the specified initialCapacity and Sorter.
	 * If {@code sorter} is {@code null}, then it is assumed that the {@link Executable} objects
	 * do not need to be sorted.
	 *
	 * @param initialCapacity The initial capacity for instantiating the internal List
	 * @param sorter The {@link Sorter} to use; may be {@code null} to indicate that the
	 *               {@link Executable} objects do not need to be sorted.
	 */
	public ExecutableList(int initialCapacity, ExecutableList.Sorter<E> sorter) {
		if ( sorter == null ) {
			sortSupport = new NoSortSupport();
		}
		else {
			sortSupport = new SorterSortSupport( sorter );
		}
		this.executables = new ArrayList<E>( initialCapacity );
		this.querySpaces = null;
	}

	/**
	 * Creates a new ExecutableList with the specified initialCapacity. If a specific
	 * {@link ExecutableList.Sorter} is required, then one of the following constructors
	 * should be used instead: {@link #ExecutableList(ExecutableList.Sorter)}
	 * or {@link #ExecutableList(int, ExecutableList.Sorter)}.
	 * <p/>
	 * If {@code requiresSorting} is {@code false}, then it is assumed that the {@link Executable}
	 * objects do not need to be sorted.
	 *
	 * @param initialCapacity The initial capacity for instantiating the internal List
	 * @param requiresSorting true indicates a default (natural ordering) sorter for sorting
	 *                       {@link Executable} objects; false indicates that the
	 *                       {@link Executable} objects do not need to be sorted.
	 */
	public ExecutableList(int initialCapacity, boolean requiresSorting) {
		if ( requiresSorting ) {
			sortSupport = new NaturalOrderingSortSupport();
		}
		else {
			sortSupport = new NoSortSupport();
		}
		this.executables = new ArrayList<E>( initialCapacity );
		this.querySpaces = null;
	}

	/**
	 * Lazily constructs the querySpaces affected by the actions in the list.
	 *
	 * @return the querySpaces affected by the actions in this list
	 */
	public Set<Serializable> getQuerySpaces() {
		if ( querySpaces == null ) {
			for ( E e : executables ) {
				Serializable[] propertySpaces = e.getPropertySpaces();
				if ( propertySpaces != null && propertySpaces.length > 0 ) {
					if( querySpaces == null ) {
						querySpaces = new HashSet<Serializable>();
					}
					Collections.addAll( querySpaces, propertySpaces );
				}
			}
			if( querySpaces == null ) {
				return Collections.emptySet();
			}
		}
		return querySpaces;
	}

	/**
	 * @return true if the list is empty.
	 */
	public boolean isEmpty() {
		return executables.isEmpty();
	}

	/**
	 * Removes the entry at position index in the list.
	 * 
	 * @param index The index of the element to remove
	 *
	 * @return the entry that was removed
	 */
	public E remove(int index) {
		// removals are generally safe in regards to sorting...

		final E e = executables.remove( index );

		// If the executable being removed defined query spaces we need to recalculate the overall query spaces for
		// this list.  The problem is that we don't know how many other executable instances in the list also
		// contributed those query spaces as well.
		//
		// An alternative here is to use a "multiset" which is a specialized set that keeps a reference count
		// associated to each entry.  But that is likely overkill here.
		if ( e.getPropertySpaces() != null && e.getPropertySpaces().length > 0 ) {
			querySpaces = null;
		}
		return e;
	}

	/**
	 * Clears the list of executions.
	 */
	public void clear() {
		executables.clear();
		querySpaces = null;
		sortSupport.clear();
	}

	/**
	 * Removes the last n entries from the list.
	 * 
	 * @param n The number of elements to remove.
	 */
	public void removeLastN(int n) {
		if ( n > 0 ) {
			int size = executables.size();
			for ( Executable e : executables.subList( size - n, size ) ) {
				if ( e.getPropertySpaces() != null && e.getPropertySpaces().length > 0 ) {
					// querySpaces could now be incorrect
					querySpaces = null;
					break;
				}
			}
			executables.subList( size - n, size ).clear();
		}
	}

	/**
	 * Add an Executable to this list.
	 * 
	 * @param executable the executable to add to the list
	 *
	 * @return true if the object was added to the list
	 */
	public boolean add(E executable) {
		if ( executables.add( executable ) ) {
			sortSupport.executableAdded();
		}
		else {
			return false;
		}

		Serializable[] querySpaces = executable.getPropertySpaces();
		if ( this.querySpaces != null && querySpaces != null ) {
			Collections.addAll( this.querySpaces, querySpaces );
		}

		return true;
	}

	/**
	 * If a {@link Sorter} was provided to the constructor, then it is used
	 * to sort the list; otherwise, if sorting is required, the list is sorted
	 * using the natural ordering.
	 */
	@SuppressWarnings("unchecked")
	public void sort() {
		sortSupport.sort();
	}

	/**
	 * @return the current size of the list
	 */
	public int size() {
		return executables.size();
	}

	/**
	 * @param index The index of the element to retrieve
	 *
	 * @return The element at specified index
	 */
	public E get(int index) {
		return executables.get( index );
	}

	/**
	 * Returns an iterator for the list. Wraps the list just in case something tries to modify it.
	 * 
	 * @return an unmodifiable iterator
	 */
	@Override
	public Iterator<E> iterator() {
		return Collections.unmodifiableList( executables ).iterator();
	}

	/**
	 * Write this list out to the given stream as part of serialization
	 * 
	 * @param oos The stream to which to serialize our state
	 */
	@Override
	public void writeExternal(ObjectOutput oos) throws IOException {
		oos.writeBoolean( sortSupport.isSorted() );

		oos.writeInt( executables.size() );
		for ( E e : executables ) {
			oos.writeObject( e );
		}

		// if the spaces are initialized, write them out for usage after deserialization
		if ( querySpaces == null ) {
			oos.writeInt( -1 );
		}
		else {
			oos.writeInt( querySpaces.size() );
			// these are always String, why we treat them as Serializable instead is beyond me...
			for ( Serializable querySpace : querySpaces ) {
				oos.writeUTF( querySpace.toString() );
			}
		}
	}

	/**
	 * Read this object state back in from the given stream as part of de-serialization
	 * 
	 * @param in The stream from which to read our serial state
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sortSupport.setSorted( in.readBoolean() );

		final int numberOfExecutables = in.readInt();
		executables.ensureCapacity( numberOfExecutables );
		if ( numberOfExecutables > 0 ) {
			for ( int i = 0; i < numberOfExecutables; i++ ) {
				E e = (E) in.readObject();
				executables.add( e );
			}
		}

		final int numberOfQuerySpaces = in.readInt();
		if ( numberOfQuerySpaces < 0 ) {
			this.querySpaces = null;
		}
		else {
			querySpaces = new HashSet<Serializable>( CollectionHelper.determineProperSizing( numberOfQuerySpaces ) );
			for ( int i = 0; i < numberOfQuerySpaces; i++ ) {
				querySpaces.add( in.readUTF() );
			}
		}
	}

	/**
	 * Allow the Executables to re-associate themselves with the Session after deserialization.
	 * 
	 * @param session The session to which to associate the Executables
	 */
	public void afterDeserialize(SessionImplementor session) {
		for ( E e : executables ) {
			e.afterDeserialize( session );
		}
	}

	public String toString() {
		return "ExecutableList{size=" + executables.size() + "}";
	}

	private interface SortSupport<E extends Executable & Comparable & Serializable> {
		void executableAdded();
		boolean isSorted();
		void setSorted(boolean sorted);
		void sort();
		void clear();
	}

	private class SorterSortSupport implements SortSupport<E> {
		private final Sorter<E> sorter;
		private boolean sorted = true;

		private SorterSortSupport(Sorter<E> sorter) {
			this.sorter = sorter;
		}

		public void setSorted(boolean sorted) {
			this.sorted = sorted;
		}

		@Override
		public void executableAdded() {
			// we don't have intrinsic insight into the sorter's algorithm;
			// if there is only one element, then we can assume it is still sorted;
			// otherwise, invalidate sorting
			sorted = executables.size() < 2;
		}

		@Override
		public boolean isSorted() {
			return sorted;
		}

		@Override
		public void sort() {
			if ( sorted ) {
				return;
			}
			sorter.sort( executables );
			sorted = true;
		}

		@Override
		public void clear() {
			sorted = true;
		}
	}

	private class NaturalOrderingSortSupport implements SortSupport<E> {
		public boolean sorted = true;

		@Override
		@SuppressWarnings("unchecked")
		public void executableAdded() {
			// if it's not sorted or fewer than 2 elements, then there is nothing to check.
			if ( !sorted || executables.size() < 2 ) {
				return;
			}
			// the value was added to the end of the list.  So check the comparison between the
			// executable that was previously at the end of the list with the last executable
			// using the Comparable contract
			final E previousLast = executables.get( executables.size() - 2 );
			final E last = executables.get( executables.size() - 1 );
			sorted = previousLast.compareTo( last ) < 0;
		}

		@Override
		public boolean isSorted() {
			return sorted;
		}

		@Override
		public void setSorted(boolean sorted) {
			this.sorted = sorted;
		}

		@Override
		public void sort() {
			if ( sorted ) {
				return;
			}
			Collections.sort( executables );
			sorted = true;
		}

		@Override
		public void clear() {
			sorted = true;
		}
	}

	private class NoSortSupport implements SortSupport<E> {

		@Override
		public void executableAdded() {
			// do nothing
		}

		@Override
		public boolean isSorted() {
			return false;
		}

		@Override
		public void setSorted(boolean sorted) {
			// ignore
		}

		@Override
		public void sort() {
			// do nothing
		}

		@Override
		public void clear() {
			// do nothing
		}
	}
}
