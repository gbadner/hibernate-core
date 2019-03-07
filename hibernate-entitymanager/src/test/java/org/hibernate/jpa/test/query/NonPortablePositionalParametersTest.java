/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;

import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */

public class NonPortablePositionalParametersTest extends BaseEntityManagerFunctionalTestCase{

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Dog.class };
	}

	@Test
	public void test() {

		EntityManager entitymanager = getOrCreateEntityManager();
		entitymanager.getTransaction().begin();
		Dog sherman = new Dog();
		sherman.name = "Sherman";
		sherman.address = "999 Golden Brick Road, Starsville, HE";
		entitymanager.persist( sherman );
		entitymanager.getTransaction().commit();
		entitymanager.close();

		entitymanager = getOrCreateEntityManager();
		entitymanager.getTransaction().begin();
		Dog goodBoy =(Dog) entitymanager.createQuery( "from Dog where name = ?0" )
				.setParameter( 0, sherman.name )
				.getSingleResult();
		assertNotNull( goodBoy );
		assertEquals( sherman.name, goodBoy.name );
		assertEquals( sherman.address, goodBoy.address );
		entitymanager.getTransaction().commit();
		entitymanager.close();

		entitymanager = getOrCreateEntityManager();
		entitymanager.getTransaction().begin();
		goodBoy = (Dog) entitymanager.createQuery( "from Dog where name = ?5" )
				.setParameter( 5, sherman.name )
				.getSingleResult();
		assertNotNull( goodBoy );
		assertEquals( sherman.name, goodBoy.name );
		assertEquals( sherman.address, goodBoy.address );
		entitymanager.getTransaction().commit();
		entitymanager.close();

		entitymanager = getOrCreateEntityManager();
		entitymanager.getTransaction().begin();
		goodBoy = (Dog) entitymanager.createQuery( "from Dog where name = ?0 and address = ?5" )
				.setParameter( 0, sherman.name )
				.setParameter( 5, sherman.address )
				.getSingleResult();
		assertNotNull( goodBoy );
		assertEquals( sherman.name, goodBoy.name );
		assertEquals( sherman.address, goodBoy.address );
		entitymanager.getTransaction().commit();
		entitymanager.close();
	}

	@Entity(name = "Dog")
	public static class Dog {
		@Id
		private String name;
		private String address;
	}
}