/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.notfound;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
public class NotFoundDerivedCompositeIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Person.class, PersonInfo.class, OtherInfo.class };
	}

	@Test
	public void test() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Person person = new Person( 1, 2 );
		person.personInfo = new PersonInfo( 1, 2 );
		person.otherInfo = new OtherInfo( 1, 2 );
		em.persist( person );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.find( PersonInfo.class, person.personInfo.id ) );
		em.flush();

		person = em.find( Person.class, person.id );

		assertNotNull( person );
		assertEquals( 1, person.id.v1 );
		assertEquals( 2, person.id.v2 );
		assertNull( person.personInfo );
		assertNotNull( person.otherInfo );
		assertEquals( 1, person.otherInfo.id.v1 );
		assertEquals( 2, person.otherInfo.id.v2 );

		em.getTransaction().commit();
		em.close();

	}

	@Entity(name="Person")
	public static class Person {

		@EmbeddedId
		private PK id;

		@OneToOne(optional = true, cascade = CascadeType.ALL)
		@JoinColumns(
				foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT ),
				value = {
						@JoinColumn( name = "v1", referencedColumnName = "v1", updatable = false, insertable = false ),
						@JoinColumn( name = "v2", referencedColumnName = "v2", updatable = false, insertable = false)
				}
		)
		@NotFound(action = NotFoundAction.IGNORE)
		private PersonInfo personInfo;

		@OneToOne(optional = true, cascade = CascadeType.ALL)
		@JoinColumns(
				foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT ),
				value = {
						@JoinColumn( name = "v1", referencedColumnName = "v1", updatable = false, insertable = false ),
						@JoinColumn( name = "v2", referencedColumnName = "v2", updatable = false, insertable = false)
				}
		)
		@NotFound(action = NotFoundAction.IGNORE)
		private OtherInfo otherInfo;

		Person() {}

		Person(int v1, int v2) {
			id = new PK();
			id.v1 = v1;
			id.v2 = v2;
		}
	}

	@Entity(name = "PersonInfo")
	public static class PersonInfo {
		@EmbeddedId
		private PK id;

		PersonInfo() {}

		PersonInfo(int v1, int v2) {
			id = new PK();
			id.v1 = v1;
			id.v2 = v2;
		}
	}

	@Entity(name = "OtherInfo")
	public static class OtherInfo {
		@EmbeddedId
		private PK id;

		OtherInfo() {}

		OtherInfo(int v1, int v2) {
			id = new PK();
			id.v1 = v1;
			id.v2 = v2;
		}

	}

	@Embeddable
	public static class PK implements Serializable {
		private int v1;
		private int v2;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			PK pk = (PK) o;

			if ( v1 != pk.v1 ) {
				return false;
			}
			return v2 == pk.v2;

		}

		@Override
		public int hashCode() {
			int result = v1;
			result = 31 * result + v2;
			return result;
		}
	}
}
