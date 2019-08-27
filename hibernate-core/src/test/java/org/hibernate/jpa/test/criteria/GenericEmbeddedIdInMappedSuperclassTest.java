/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.MappedSuperclass;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

public class GenericEmbeddedIdInMappedSuperclassTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				ConcreteEntityOne.class,
				ConcreteEntityTwo.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11955")
	public void test() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery cq = cb.createQuery(ConcreteEntityOne.class);
		Root<ConcreteEntityOne> root = cq.from(ConcreteEntityOne.class);
		Path<ConcreteKeyOne> id = root.get("key");
		Path<String> name = id.get("one");
		Path<String> name2 = root.get("key").get("one");
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@MappedSuperclass
	public static abstract class AbstractEntity<Key extends AbstractKey> implements Serializable {
		@EmbeddedId
		protected Key key;

		public Key getKey() {
			return key;
		}

		public void setKey(Key key) {
			this.key = key;
		}
	}

	public static class AbstractKey implements Serializable {
	}

	@Entity(name = "ConcreteEntityOne")
	public static class ConcreteEntityOne extends AbstractEntity<ConcreteKeyOne> {

	}

	@Entity(name = "ConcreteEntityTwo")
	public static class ConcreteEntityTwo extends AbstractEntity<ConcreteKeyTwo> {

	}

	@Embeddable
	public static class ConcreteKeyOne extends AbstractKey {
		private static final long serialVersionUID = 1l;

		@Column(nullable=false)
		public String one;

		public String getOne() {
			return one;
		}

		public void setOne(String value) {
			this.one = value;
		}
	}

	@Embeddable
	public static class ConcreteKeyTwo extends AbstractKey {
		private static final long serialVersionUID = 1l;

		@Column(nullable=false)
		public String two;

		public String getTwo() {
			return two;
		}

		public void setTwo(String value) {
			two = value;
		}
	}

}