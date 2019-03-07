/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Query;
import org.hibernate.QueryParameterException;
import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */

public class NonPortablePositionalParametersTest extends BaseNonConfigCoreFunctionalTestCase{

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Dog.class };
	}

	@Test
	public void test() {

		Session session = openSession();
		session.beginTransaction();
		Dog sherman = new Dog();
		sherman.name = "Sherman";
		sherman.address = "999 Golden Brick Road, Starsville, HE";
		session.persist( sherman );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		try {
			Query query = session.createQuery( "from Dog where name = ?0" )
					.setParameter( 0, sherman.name );
			fail();
		}
		catch (QueryParameterException ex) {
			session.getTransaction().rollback();
		}
		finally {
			session.close();
		}

		session = openSession();
		session.beginTransaction();
		try {
			Query query = session.createQuery( "from Dog where name = ?5" )
					.setParameter( 5, sherman.name );
			fail();
		}
		catch (QueryParameterException ex) {
			session.getTransaction().rollback();
		}
		finally {
			session.close();
		}

		session = openSession();
		session.beginTransaction();
		try {
			Query query = session.createQuery(
					"from Dog where name = ?0 and address = ?5"
			).setParameter( 0, sherman.name );
			fail();
		}
		catch (QueryParameterException ex) {
			session.getTransaction().rollback();
		}
		finally {
			session.close();
		}
	}

	@Entity(name = "Dog")
	public static class Dog {
		@Id
		private String name;
		private String address;
	}
}