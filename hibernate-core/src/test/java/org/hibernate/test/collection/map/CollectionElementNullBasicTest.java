/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class CollectionElementNullBasicTest extends BaseCoreFunctionalTestCase {

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
		e.aMap.put( "null", null );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 0, e.aMap.size() );
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
		e.aMap.put( "abc", "def" );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 1, e.aMap.size() );
		assertEquals( 1, getCollectionElementRows( e.id ).size() );
		e.aMap.put( "abc", null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 0, e.aMap.size() );
		assertEquals( 0, getCollectionElementRows( e.id ).size() );
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testUpdateNonNullValueToNullToNonNull() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		e.aMap.put( "abc", "def" );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 1, e.aMap.size() );
		assertEquals( 1, getCollectionElementRows( e.id ).size() );
		e.aMap.put( "abc", null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 0, e.aMap.size() );
		assertEquals( 0, getCollectionElementRows( e.id ).size() );
		e.aMap.put( "abc", "not null" );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 1, e.aMap.size() );
		assertEquals( 1, getCollectionElementRows( e.id ).size() );
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	private List getCollectionElementRows(int id) {
		Session s = openSession();
		s.getTransaction().begin();
		List collectionElementRows =
				s.createSQLQuery( "SELECT aMap FROM AnEntity_aMap where AnEntity_id = " + id ).list();
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
		@CollectionTable(name = "AnEntity_aMap", joinColumns = { @JoinColumn( name = "AnEntity_id" ) })
		private Map<String, String> aMap = new HashMap<String, String>();
	}
}
