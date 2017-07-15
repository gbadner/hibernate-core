/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public  class MapElementNullEmbeddableEmptyDisabledTest extends BaseCoreFunctionalTestCase {

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
		assertEquals( 0, e.aMap.size() );
		assertEquals( 0, getCollectionElementRows( e.id ).size() );
		e.aMap.put( "null", null );
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
		e.aMap.put( "abc", new Thing( "def", "ghi" ) );
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
		e.aMap.put( "abc", new Thing( "def", "ghi" ) );
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
		e.aMap.put( "abc", new Thing( "jkl", "mno" ) );
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

	@Test
	public void testUpdateNonNullValueToNullWithExtraValue() {
		Session s = openSession();
		s.getTransaction().begin();
		AnEntity e = new AnEntity();
		e.aMap.put( "abc", new Thing( "def", "ghi" ) );
		e.aMap.put( "zyx", new Thing( "wvu", "tsr" ) );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 2, e.aMap.size() );
		assertEquals( 2, getCollectionElementRows( e.id ).size() );
		e.aMap.put( "abc", null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (AnEntity) s.get( AnEntity.class, e.id );
		assertEquals( 1, e.aMap.size() );
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
		List results = s.createSQLQuery( "SELECT value1, value2 FROM AnEntity_aMap where AnEntity_id = " + id ).list();
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
		@CollectionTable(name = "AnEntity_aMap", joinColumns = { @JoinColumn( name = "AnEntity_id" ) })
		private Map<String, Thing> aMap = new HashMap<String, Thing>();
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
