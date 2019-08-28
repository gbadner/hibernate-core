/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ejb.criteria;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.ejb.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

public class GenericPropertiesInMappedSuperclassTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				ConcreteEntityOne.class,
				ConcreteEntityTwo.class,
				One.class,
				Two.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11955")
	public void test() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		CriteriaBuilder cbOne = entityManager.getCriteriaBuilder();
		CriteriaQuery cqOne = cbOne.createQuery( ConcreteEntityOne.class );
		Root<ConcreteEntityOne> rootOne = cqOne.from( ConcreteEntityOne.class );
		Path<One> one = rootOne.get( "anEntity" );
		Path<String> oneValue = one.get( "oneValue" );


		CriteriaBuilder cbTwo = entityManager.getCriteriaBuilder();
		CriteriaQuery cqTwo = cbTwo.createQuery( ConcreteEntityTwo.class );
		Root<ConcreteEntityTwo> rootTwo = cqTwo.from( ConcreteEntityTwo.class );
		Path<Two> two = rootTwo.get( "anEntity" );
		Path<Long> twoValue = two.get( "twoValue" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@MappedSuperclass
	public static abstract class AbstractEntity<T> {

		@Id
		private int id;

		@ManyToOne
		protected T anEntity;
	}

	@Entity(name = "ConcreteEntityOne")
	public static class ConcreteEntityOne extends AbstractEntity<One> {

	}

	@Entity(name = "One")
	public static class One {
		@Id
		private int id;

		private String oneValue;
	}

	@Entity(name = "ConcreteEntityTwo")
	public static class ConcreteEntityTwo extends AbstractEntity<Two> {
	}

	@Entity(name = "Two")
	public static class Two {
		@Id
		private int id;

		private Long twoValue;
	}
}