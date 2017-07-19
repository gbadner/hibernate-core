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
import javax.persistence.Embeddable;
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
public class ListElementEmptyEmbeddableEmptyDisabledTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Test
	public void testPersistEmptyValue() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		e.aList.add( new Thing() );
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
	public void addEmptyValue() {
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
		e.aList.add( new Thing() );
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
	public void testUpdateNonEmptyValueToEmpty() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		e.aList.add( new Thing( "def", "ghi" ) );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 1, e.aList.size() );
		assertEquals( 1, getCollectionElementRows( e.id ).size() );
		e.aList.set( 0, new Thing() );
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
	public void testUpdateNonEmptyValueToNonEmpty() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		e.aList.add( new Thing( "def", "ghi" ) );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 1, e.aList.size() );
		assertEquals( 1, getCollectionElementRows( e.id ).size() );
		e.aList.set( 0, new Thing() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 0, e.aList.size() );
		assertEquals( 0, getCollectionElementRows( e.id ).size() );
		// can't set at index 0 because collection is empty; instead add
		e.aList.add( new Thing( "pqr", "stu" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 1, e.aList.size() );
		assertEquals( 1, getCollectionElementRows( e.id ).size() );
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testUpdateNonEmptyValueToEmptyWithExtraValue() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		e.aList.add( new Thing( "def", "ghi" ) );
		e.aList.add( new Thing( "wvu", "tsr" ) );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 2, e.aList.size() );
		assertEquals( 2, getCollectionElementRows( e.id ).size() );
		e.aList.set( 0, new Thing() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 2, e.aList.size() );
		List<Thing> things = getCollectionElementRows( e.id );
		assertEquals( 1, things.size() );
		assertEquals( "wvu", things.get( 0 ).value1 );
		assertEquals( "tsr", things.get( 0 ).value2 );
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	private List<Thing> getCollectionElementRows(int id) {
		Session s = openSession();
		s.getTransaction().begin();
		List results = s.createSQLQuery( "SELECT value1, value2 FROM AnEntity_aList where AnEntity_id = " + id ).list();
		List<Thing> things = new ArrayList<Thing>();
		for ( Object result : results ) {
			Object[] resultArray = (Object[] ) result;
			things.add( new Thing( (String) resultArray[0], (String) resultArray[1] ) );
		}
		s.getTransaction().commit();
		s.close();
		return things;
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
		private List<Thing> aList = new ArrayList<Thing>();
	}

	@Embeddable
	public static class Thing {
		private String value1;
		private String value2;

		public Thing() {
		}

		public Thing(String value1, String value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

	}
}
