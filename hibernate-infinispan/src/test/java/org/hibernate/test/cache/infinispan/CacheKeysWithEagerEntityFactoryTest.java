package org.hibernate.test.cache.infinispan;

import java.io.Serializable;
import java.util.Iterator;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.SessionFactory;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.CacheImplementor;
import org.hibernate.jpa.AvailableSettings;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.junit.Rule;
import org.junit.Test;

import org.infinispan.Cache;

import static org.hibernate.test.cache.infinispan.util.TxUtil.withTxSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CacheKeysWithEagerEntityFactoryTest extends BaseUnitTestCase {
	@Rule
	public InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

	private SessionFactory getSessionFactory(String cacheKeysFactory) {
		Configuration configuration = new Configuration()
				.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" )
				.setProperty( Environment.CACHE_REGION_FACTORY, TestInfinispanRegionFactory.class.getName() )
				.setProperty( Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "transactional" )
				.setProperty( AvailableSettings.SHARED_CACHE_MODE, "ALL" )
				.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		if ( cacheKeysFactory != null ) {
			configuration.setProperty( Environment.CACHE_KEYS_FACTORY, cacheKeysFactory );
		}
		configuration.addAnnotatedClass( TheEntity.class );
		configuration.addAnnotatedClass( OtherEntity.class );
		configuration.addAnnotatedClass( DifferentEntity.class );
		return configuration.buildSessionFactory();
	}

	@Test
	public void testNotSet() throws Exception {
		test( null, "CacheKeyImplementation" );
	}

	@Test
	public void testDefault() throws Exception {
		test( DefaultCacheKeysFactory.SHORT_NAME, "CacheKeyImplementation" );
	}

	@Test
	public void testDefaultClass() throws Exception {
		test( DefaultCacheKeysFactory.class.getName(), "CacheKeyImplementation" );
	}

	@Test
	public void testSimple() throws Exception {
		test( SimpleCacheKeysFactory.SHORT_NAME, TheEntityPK.class.getSimpleName() );
	}

	@Test
	public void testSimpleClass() throws Exception {
		test( SimpleCacheKeysFactory.class.getName(), TheEntityPK.class.getSimpleName() );
	}

	private void test(String cacheKeysFactory, String keyClassName) throws Exception {
		SessionFactory sessionFactory = getSessionFactory( cacheKeysFactory );
		withTxSession(
				false, sessionFactory, s -> {
					OtherEntity otherEntity = new OtherEntity();
					otherEntity.id = 1;
					s.persist( otherEntity );
					DifferentEntity differentEntity = new DifferentEntity();
					differentEntity.id = 2;
					s.persist( differentEntity );
					otherEntity.differentEntity = differentEntity;
					TheEntity theEntity = new TheEntity();
					theEntity.theEntityPK = new TheEntityPK();
					theEntity.theEntityPK.intVal = 1;
					theEntity.theEntityPK.otherEntity = otherEntity;
					s.persist( theEntity );
				}
		);

		withTxSession(
				false, sessionFactory, s -> {
					OtherEntity otherEntity = s.get( OtherEntity.class, 1 );
					TheEntity theEntity = new TheEntity();
					theEntity.theEntityPK = new TheEntityPK();
					theEntity.theEntityPK.intVal = 2;
					theEntity.theEntityPK.otherEntity = otherEntity;
					s.persist( theEntity );
				}
		);


		TestInfinispanRegionFactory regionFactory = (TestInfinispanRegionFactory) ( (CacheImplementor) sessionFactory.getCache() )
				.getRegionFactory();
		Cache<Object, Object> cache = regionFactory.getCacheManager().getCache( TheEntity.class.getName() );
		Iterator<Object> iterator = cache.getAdvancedCache().getDataContainer().keySet().iterator();
		assertTrue( iterator.hasNext() );
		Object key1 = iterator.next();
		assertTrue( iterator.hasNext() );
		Object key2 = iterator.next();
		assertFalse( iterator.hasNext() );
		assertEquals( keyClassName, key1.getClass().getSimpleName() );
		assertEquals( keyClassName, key2.getClass().getSimpleName() );
		final TheEntityPK pk1 = getTheEntityPKFromCacheKey( key1 );
		final TheEntityPK pk2 = getTheEntityPKFromCacheKey( key2 );
		assertFalse( pk1.otherEntity == pk2.otherEntity );
		assertTrue( pk1.otherEntity.equals( pk2.otherEntity ) );

		withTxSession(
				false, sessionFactory, s -> {
					OtherEntity otherEntity = s.get( OtherEntity.class, 1 );
					TheEntityPK theEntityPK1 = new TheEntityPK();
					theEntityPK1.intVal = 1;
					theEntityPK1.otherEntity = otherEntity;
					TheEntity theEntity1 = s.get( TheEntity.class, theEntityPK1 );
					assertSame( otherEntity, theEntity1.theEntityPK.otherEntity );
					TheEntityPK theEntityPK2 = new TheEntityPK();
					theEntityPK2.intVal = 2;
					theEntityPK2.otherEntity = otherEntity;
					TheEntity theEntity2 = s.get( TheEntity.class, theEntityPK2 );
					assertSame( otherEntity, theEntity2.theEntityPK.otherEntity );

				}
		);
	}

	private TheEntityPK getTheEntityPKFromCacheKey(Object cacheKey) {
		if ( TheEntityPK.class.isInstance( cacheKey ) ) {
			return (TheEntityPK) SimpleCacheKeysFactory.INSTANCE.getEntityId( cacheKey );
		}
		else {
			return (TheEntityPK) DefaultCacheKeysFactory.INSTANCE.getEntityId( cacheKey );
		}
	}

	@Entity(name = "TheEntity")
	public static class TheEntity {

		@EmbeddedId
		private TheEntityPK theEntityPK;

	}

	@Embeddable
	public static class TheEntityPK implements Serializable {
		private int intVal;

		@ManyToOne(fetch = FetchType.EAGER)
		private OtherEntity otherEntity;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			TheEntityPK that = (TheEntityPK) o;

			if ( intVal != that.intVal ) {
				return false;
			}
			return otherEntity.equals( that.otherEntity );
		}

		@Override
		public int hashCode() {
			int result = intVal;
			result = 31 * result + otherEntity.hashCode();
			return result;
		}
	}

	@Entity(name = "OtherEntity")
	public static class OtherEntity {
		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.EAGER)
		private DifferentEntity differentEntity;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			OtherEntity that = (OtherEntity) o;

			return id.equals( that.id );

		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}

	@Entity(name = "DifferentEntity")
	public static class DifferentEntity {
		@Id
		private Integer id;
	}
}
