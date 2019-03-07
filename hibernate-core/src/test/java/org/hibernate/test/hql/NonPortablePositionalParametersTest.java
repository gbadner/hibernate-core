/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
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

		Dog sherman = doInHibernate(
			this::sessionFactory, session -> {
					Dog goodBoy = new Dog();
					goodBoy.name = "Sherman";
					goodBoy.address = "999 Golden Brick Road, Starsville, HE";
					session.persist( goodBoy );
					return goodBoy;
			}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					Dog goodBoy = session.createQuery( "from Dog where name = ?0", Dog.class )
							.setParameter( 0, sherman.name )
							.uniqueResult();
					assertNotNull( goodBoy );
					assertEquals( sherman.name, goodBoy.name );
					assertEquals( sherman.address, goodBoy.address );

					goodBoy = session.createQuery( "from Dog where name = ?5", Dog.class )
							.setParameter( 5, sherman.name )
							.uniqueResult();
					assertNotNull( goodBoy );
					assertEquals( sherman.name, goodBoy.name );
					assertEquals( sherman.address, goodBoy.address );

					try {
						javax.persistence.Query query = session.createQuery(
								"from Dog where name = ?0 and address = ?5",
								Dog.class
						);
						fail();
					}
					catch (IllegalArgumentException ex) {
						assertTrue( org.hibernate.QueryException.class.isInstance( ex.getCause() ));
						assertEquals(
								"Unexpected gap in ordinal parameter labels [0 -> 5] : [0,5]",
								ex.getCause().getMessage()
						);
					}
				}
		);
	}

	@Entity(name = "Dog")
	public static class Dog {
		@Id
		private String name;
		private String address;
	}
}