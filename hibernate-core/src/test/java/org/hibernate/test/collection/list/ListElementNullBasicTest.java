/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.list;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class ListElementNullBasicTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Test
	public void testPersistNullValue() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		e.aList.add( null );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 0, e.aList.size() );
		assertEquals( 0, getCollectionElementRows( e.id ).size() );
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void addNullValue() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 0, e.aList.size() );
		assertEquals( 0, getCollectionElementRows( e.id ).size() );
		e.aList.add( null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 0, e.aList.size() );
		assertEquals( 0, getCollectionElementRows( e.id ).size() );
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testUpdateNonNullValueToNull() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		e.aList.add( "def" );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 1, e.aList.size() );
		assertEquals( 1, getCollectionElementRows( e.id ).size() );
		e.aList.set( 0, null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 0, e.aList.size() );
		assertEquals( 0, getCollectionElementRows( e.id ).size() );
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testUpdateNonNullValueToNullWithExtraValue() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		e.aList.add( "def" );
		e.aList.add( "ghi" );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 2, e.aList.size() );
		assertEquals( 2, getCollectionElementRows( e.id ).size() );
		e.aList.set( 0, null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 2, e.aList.size() );
		assertEquals( 1, getCollectionElementRows( e.id ).size() );
		e.aList.set( 0, "not null" );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 2, e.aList.size() );
		assertEquals( 2, getCollectionElementRows( e.id ).size() );
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	private List getCollectionElementRows(int id) {
		Session s = openSession();
		s.getTransaction().begin();
		List collectionElementRows =
				s.createSQLQuery( "SELECT aList FROM AnEntity_aList where AnEntity_id = " + id ).list();
		s.getTransaction().commit();
		s.close();
		return collectionElementRows;
	}


	@Entity
	@Table(name="AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;

		@ElementCollection
		@CollectionTable(name = "AnEntity_aList", joinColumns = { @JoinColumn( name = "AnEntity_id" ) })
		@OrderColumn
		private List<String> aList = new ArrayList<String>();
	}
}
