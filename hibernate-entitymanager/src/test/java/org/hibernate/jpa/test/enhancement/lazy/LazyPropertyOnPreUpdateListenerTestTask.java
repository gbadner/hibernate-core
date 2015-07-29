/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.enhancement.lazy;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.test.enhancement.AbstractEnhancerTestTask;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

 /**
  * Test for HHH-7573.
  * Load some test data into an entity which has a lazy property and a @PreUpdate callback, then reload and update a
  * non lazy field which will trigger the PreUpdate lifecycle callback.
  */
public class LazyPropertyOnPreUpdateListenerTestTask extends AbstractEnhancerTestTask {
	@Override
	protected void cleanup() {
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityWithLazyProperty.class };
	}

	@Override
	public void prepare() {
		Map<Object,Object> properties = new HashMap<Object,Object>();
		properties.put( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		properties.put( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( properties );
	}

	@Override
	public void execute() {
		EntityWithLazyProperty entity;
		EntityManager em = getOrCreateEntityManager();

		byte[] testArray = new byte[]{0x2A};

		//persist the test entity.
		em.getTransaction().begin();
		entity = new EntityWithLazyProperty();
		entity.setSomeField("TEST");
		entity.setLazyData(testArray);
		em.persist(entity);
		em.getTransaction().commit();
		em.close();

		checkLazyField(entity, testArray);

		/**
		 * Set a non lazy field, therefore the lazyData field will be LazyPropertyInitializer.UNFETCHED_PROPERTY
		 * for both state and newState so the field should not change. This should no longer cause a ClassCastException.
		 */
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		entity = em.find(EntityWithLazyProperty.class, entity.getId());
		entity.setSomeField("TEST1");
		//assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.getTransaction().commit();
		//assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.close();

		checkLazyField(entity, testArray);

		/**
		 * Set the updateLazyFieldInPreUpdate flag so that the lazy field is updated from within the
		 * PreUpdate annotated callback method. So state == LazyPropertyInitializer.UNFETCHED_PROPERTY and
		 * newState == EntityWithLazyProperty.PRE_UPDATE_VALUE. This should no longer cause a ClassCastException.
		 */
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		entity = em.find(EntityWithLazyProperty.class, entity.getId());
		entity.setUpdateLazyFieldInPreUpdate(true);
		entity.setSomeField("TEST2");
		//assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.getTransaction().commit();
		//assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.close();

		checkLazyField(entity, EntityWithLazyProperty.PRE_UPDATE_VALUE);

		/**
		 * Set the updateLazyFieldInPreUpdate flag so that the lazy field is updated from within the
		 * PreUpdate annotated callback method and also set the lazyData field directly to testArray1. When we reload we
		 * should get EntityWithLazyProperty.PRE_UPDATE_VALUE.
		 */
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		entity = em.find(EntityWithLazyProperty.class, entity.getId());
		entity.setUpdateLazyFieldInPreUpdate(true);
		//assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		entity.setLazyData(testArray);
		//assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		entity.setSomeField("TEST3");
		em.getTransaction().commit();
		em.close();

		checkLazyField( entity, EntityWithLazyProperty.PRE_UPDATE_VALUE);

	}

	private void checkLazyField(EntityWithLazyProperty entity, byte[] expected) {
		// reload the entity and check the lazy value matches what we expect.
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		entity = em.find(EntityWithLazyProperty.class, entity.getId());
		//assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData") );
		assertTrue( ArrayHelper.isEquals( expected, entity.getLazyData() ) );
		//assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.getTransaction().commit();
		em.close();
	}

}
