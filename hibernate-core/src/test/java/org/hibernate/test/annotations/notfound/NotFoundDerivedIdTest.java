/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.notfound;

import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.Session;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
public class NotFoundDerivedIdTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Person.class, PersonInfo.class, OtherInfo.class };
	}

	@Test
	public void test() {
		Session s = openSession();
		s.getTransaction().begin();
		Person person = new Person();
		person.id = 1;
		person.personInfo = new PersonInfo();
		person.personInfo.id = 1;
		person.otherInfo = new OtherInfo();
		person.otherInfo.id = 1;
		s.persist( person );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		s.delete( person.personInfo );
		s.flush();

		person = s.get( Person.class, person.id );

		assertNotNull( person );
		assertEquals( 1, person.id );
		assertNull( person.personInfo );
		assertNotNull( person.otherInfo );
		assertEquals( 1, person.otherInfo.id );

		s.getTransaction().commit();
		s.close();

	}

	@Entity(name="Person")
	public static class Person {

		@Id
		private int id;

		@OneToOne(optional = true, cascade = CascadeType.ALL)
		@JoinColumn(name = "id", updatable = false, insertable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
		@NotFound(action = NotFoundAction.IGNORE)
		private PersonInfo personInfo;

		@OneToOne(optional = true, cascade = CascadeType.ALL)
		@JoinColumn(name = "id", updatable = false, insertable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
		@NotFound(action = NotFoundAction.IGNORE)
		private OtherInfo otherInfo;
	}

	@Entity(name = "PersonInfo")
	public static class PersonInfo {
		@Id
		private int id;

	}

	@Entity(name = "OtherInfo")
	public static class OtherInfo {
		@Id
		private int id;
	}
}
