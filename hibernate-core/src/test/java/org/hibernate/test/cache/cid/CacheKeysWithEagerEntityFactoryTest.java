package org.hibernate.test.cache.cid;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class CacheKeysWithEagerEntityFactoryTest {

	@Parameterized.Parameters(name = "cacheKeysFactorySetting={0}")
	public static Collection<String> parameters() {
		return Arrays.asList(
				DefaultCacheKeysFactory.SHORT_NAME,
				SimpleCacheKeysFactory.SHORT_NAME,
				null
		);
	}

	private final SessionFactoryImplementor sessionFactoryImplementor;
	private StandardServiceRegistry ssr;

	@Parameterized.Parameter
	private String cacheKeysFactorySetting;

	public CacheKeysWithEagerEntityFactoryTest() {
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder()
				.applySetting( Environment.USE_SECOND_LEVEL_CACHE, "true" )
				.applySetting( Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "transactional" )
				.applySetting( AvailableSettings.SHARED_CACHE_MODE, "ALL" )
				.applySetting( Environment.HBM2DDL_AUTO, "create-drop" );
		if ( cacheKeysFactorySetting != null ) {
			ssrb.applySetting( Environment.CACHE_KEYS_FACTORY, cacheKeysFactorySetting );
		}
		ssr = ssrb.build();

		sessionFactoryImplementor = (SessionFactoryImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( TheEntity.class )
				.addAnnotatedClass( OtherEntity.class )
				.addAnnotatedClass( DifferentEntity.class )
				.buildMetadata()
				.buildSessionFactory();
	}

	@After
	public void tearDown() {
		sessionFactoryImplementor.close();
		ssr.close();
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

		final EntityPersister entityPersister = sessionFactory().getMetamodel().entityPersister( TheEntity.class );
		final EntityDataAccess entityDataAccess = entityPersister.getCacheAccessStrategy();

		final OtherEntity otherEntityDetached = doInHibernate(
				this::sessionFactory, session -> {
					OtherEntity otherEntity = new OtherEntity();
					otherEntity.id = 1;
					session.persist( otherEntity );
					DifferentEntity differentEntity = new DifferentEntity();
					differentEntity.id = 2;
					session.persist( differentEntity );
					otherEntity.differentEntity = differentEntity;
					TheEntity theEntity = new TheEntity();
					theEntity.theEntityPK = new TheEntityPK();
					theEntity.theEntityPK.intVal = 1;
					theEntity.theEntityPK.otherEntity = otherEntity;
					session.persist( theEntity );
					return otherEntity;
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					OtherEntity otherEntity = session.get( OtherEntity.class, 1 );
					TheEntity theEntity = new TheEntity();
					theEntity.theEntityPK = new TheEntityPK();
					theEntity.theEntityPK.intVal = 2;
					theEntity.theEntityPK.otherEntity = otherEntity;
					session.persist( theEntity );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {

					final Object cacheKey1 = entityDataAccess.generateCacheKey(
							new TheEntityPK( 1, otherEntityDetached),
							entityPersister,
							sessionFactory(),
							null
					);
					final CacheEntry cacheEntry1 = (CacheEntry) entityDataAccess.get(
							(SharedSessionContractImplementor) session,
							cacheKey1
					);
					final Object cacheKey2 = entityDataAccess.generateCacheKey(
							new TheEntityPK( 2, otherEntityDetached),
							entityPersister,
							sessionFactory(),
							null
					);
					final CacheEntry cacheEntry2 = (CacheEntry) entityDataAccess.get(
							(SharedSessionContractImplementor) session,
							cacheKey2
					);
					assertNotNull( cacheEntry1 );
					assertNotNull( cacheEntry2 );


/*							OtherEntity otherEntity = session.get( OtherEntity.class, 1 );
					TheEntityPK theEntityPK1 = new TheEntityPK();
					theEntityPK1.intVal = 1;
					theEntityPK1.otherEntity = otherEntity;
					TheEntity theEntity1 = session.get( TheEntity.class, theEntityPK1 );
					assertSame( otherEntity, theEntity1.theEntityPK.otherEntity );
					TheEntityPK theEntityPK2 = new TheEntityPK();
					theEntityPK2.intVal = 2;
					theEntityPK2.otherEntity = otherEntity;
					TheEntity theEntity2 = session.get( TheEntity.class, theEntityPK2 );
					assertSame( otherEntity, theEntity2.theEntityPK.otherEntity );
*/
				}
		);
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactoryImplementor;
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

		public TheEntityPK() {
		}

		public TheEntityPK(int intVal, OtherEntity otherEntity) {
			this.intVal = intVal;
			this.otherEntity = otherEntity;
		}

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
