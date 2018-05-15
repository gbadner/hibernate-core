/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.bugs;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransientObjectException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 *
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
public class ORMTransientObjectExceptionUnitTestCase extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				AInfo.class
		};
	}

	// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
	@Override
	protected String[] getMappings() {
		return new String[] {
//				"Foo.hbm.xml",
//				"Bar.hbm.xml"
		};
	}
	// If those mappings reside somewhere other than resources/org/hibernate/test, change this.
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
	}

	@Test
	public void testSave() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.save( a );
			fail( "should have thrown an exception" );
		}
		catch (HibernateException expected) {
			// unwrapped works in 5.1 and 5.3
			assertTrue( TransientObjectException.class.isInstance( expected ) );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testSaveOrUpdate() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.saveOrUpdate( a );
			fail( "should have thrown an exception" );
		}
		catch (HibernateException expected) {
			// unwrapped works in 5.1 and 5.3
			assertTrue( TransientObjectException.class.isInstance( expected ) );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testPersist() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.persist( a );
			fail( "should have thrown an exception" );
		}
		catch (HibernateException expected) {
			// unwrapped works in 5.1; does not work in 5.3
			assertTrue( TransientObjectException.class.isInstance( expected ) );
		}
		catch (IllegalStateException ex) {
			// wrapped in IllegalStateException works in 5.3
			assertTrue( TransientObjectException.class.isInstance( ex.getCause() ) );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testMerge() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.merge( a );
			fail( "should have thrown an exception" );
		}
		catch (HibernateException expected) {
			// unwrapped works in 5.1
			assertTrue( TransientObjectException.class.isInstance( expected ) );
		}
		catch (IllegalStateException ex) {
			// wrapped in IllegalStateException works in 5.3
			assertTrue( TransientObjectException.class.isInstance( ex.getCause() ) );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testUpdateFlush() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.id = 1;
		a.aInfo = new AInfo();
		try {
			s.update( a );
			s.flush();
			fail( "should have thrown an exception" );
		}
		catch (HibernateException expected) {
			// unwrapped works in 5.1; does not work in 5.3
			assertTrue( TransientObjectException.class.isInstance( expected ) );
		}
		catch (IllegalStateException ex) {
			// wrappped works in 5.3
			assertTrue( TransientObjectException.class.isInstance( ex.getCause() ) );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Entity(name = "A")
	public static class A {
		@Id
		private long id;

		@ManyToOne(optional = false)
		private AInfo aInfo;
	}

	@Entity(name = "AInfo")
	public static class AInfo {
		@Id
		@GeneratedValue
		private long id;
	}
}
