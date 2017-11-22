/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.inheritance.mixed;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gail Badner
 */
public class MixedSameSingleTableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				B.class,
				A1.class,
				B1.class
		};
	}

	@Test
	public void test() {
	}

	@Entity(name="A")
	@Table(name="A_TABLE")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorValue( "A" )
	public static class A {
		@Id
		private int id;
	}

	@Entity(name="B")
	@DiscriminatorValue( "B" )
	public static class B extends A {

	}

	@Entity(name="A1")
	@Table(name="A_TABLE")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorValue( "A" )
	public static class A1 extends A {

	}

	@Entity(name="B1")
	@DiscriminatorValue( "B" )
	public static class B1 extends A1 {
	}
}
