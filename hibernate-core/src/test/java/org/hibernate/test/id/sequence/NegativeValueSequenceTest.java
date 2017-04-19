/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.sequence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class NegativeValueSequenceTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Test
	public void testGenerationPastBound() {
		Session session = openSession();
		session.getTransaction().begin();
		for ( Integer i=-10 ; i >= -15 ; i--) {
			TheEntity theEntity = new TheEntity();
			session.persist( theEntity );
			assertEquals( i, theEntity.id );
		}
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete TheEntity" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Entity( name = "TheEntity" )
	@Table( name = "TheEntity" )
	public static class TheEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_GENERATOR")
		@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "ENTITY_SEQ", initialValue= -10, allocationSize = -1)
		public Integer id;
	}

}
