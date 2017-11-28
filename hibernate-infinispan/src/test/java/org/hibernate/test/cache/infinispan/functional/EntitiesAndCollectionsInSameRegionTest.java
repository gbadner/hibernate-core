/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Gail Badner
 */
public class EntitiesAndCollectionsInSameRegionTest extends SingleNodeTestCase {
	private static final String REGION_NAME = "ARegion";

	private final AnEntity anEntity;
	private final AnotherEntity anotherEntity;

	public EntitiesAndCollectionsInSameRegionTest() {
		anEntity = new AnEntity();
		anEntity.id = 1;
		anEntity.values.add( "abc" );

		anotherEntity = new AnotherEntity();
		anotherEntity.id = 1;
		anotherEntity.values.add( 123 );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class,
				AnotherEntity.class
		};
	}

	// cache setting are already mapped via annotations, so no need to do anything here
	protected void applyCacheSettings(Configuration configuration) {
	}

	@Before
	public void setup() throws Exception {

		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		beginTx();
		try {
			Session s = openSession();
			s.getTransaction().begin();
			s.persist( anEntity );
			s.persist( anotherEntity );
			s.getTransaction().commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		// Then entities should have been cached, but not their collections.
		SecondLevelCacheStatistics cacheStatistics = stats.getSecondLevelCacheStatistics( REGION_NAME );
		assertEquals( 0, cacheStatistics.getMissCount() );
		assertEquals( 0, cacheStatistics.getHitCount() );
		assertEquals( 2, cacheStatistics.getPutCount() );

		stats.clear();
	}

	@After
	public void cleanup() throws Exception{
		beginTx();
		try {
			Session s = openSession();
			s.getTransaction().begin();
			s.delete( s.get( AnEntity.class, 1 ) );
			s.delete( s.get( AnotherEntity.class, 1 ) );
			s.getTransaction().commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10418")
	public void testEntitiesAndCollections() throws Exception {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		beginTx();
		try {
			Session s = openSession();
			s.getTransaction().begin();

			stats.clear();

			AnEntity anEntity1 = (AnEntity) s.get( AnEntity.class, anEntity.id );

			SecondLevelCacheStatistics cacheStatistics = stats.getSecondLevelCacheStatistics( REGION_NAME );

			// anEntity1 was cached when it was persisted
			assertEquals( 0, cacheStatistics.getMissCount() );
			assertEquals( 1, cacheStatistics.getHitCount() );
			assertEquals( 0, cacheStatistics.getPutCount() );

			stats.clear();

			assertFalse( Hibernate.isInitialized( anEntity1.values ) );
			Hibernate.initialize( anEntity1.values );

			// anEntity1.values gets cached when it gets loadead
			cacheStatistics = stats.getSecondLevelCacheStatistics( REGION_NAME );
			assertEquals( 1, cacheStatistics.getMissCount() );
			assertEquals( 0, cacheStatistics.getHitCount() );
			assertEquals( 1, cacheStatistics.getPutCount() );

			stats.clear();

			AnotherEntity anotherEntity1 = (AnotherEntity) s.get( AnotherEntity.class, anotherEntity.id );

			// anotherEntity1 was cached when it was persisted
			cacheStatistics = stats.getSecondLevelCacheStatistics( REGION_NAME );
			assertEquals( 0, cacheStatistics.getMissCount() );
			assertEquals( 1, cacheStatistics.getHitCount() );
			assertEquals( 0, cacheStatistics.getPutCount() );

			stats.clear();

			assertFalse( Hibernate.isInitialized( anotherEntity1.values ) );
			Hibernate.initialize( anotherEntity1.values );

			// anotherEntity1.values gets cached when it gets loadead
			cacheStatistics = stats.getSecondLevelCacheStatistics( REGION_NAME );
			assertEquals( 1, cacheStatistics.getMissCount() );
			assertEquals( 0, cacheStatistics.getHitCount() );
			assertEquals( 1, cacheStatistics.getPutCount() );

			s.getTransaction().commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}

		// The entities and their collections should all be cached now.

		beginTx();
		try {
			Session s = openSession();
			s.getTransaction().begin();

			stats.clear();

			AnEntity anEntity1 = (AnEntity) s.get( AnEntity.class, anEntity.id );

			SecondLevelCacheStatistics cacheStatistics = stats.getSecondLevelCacheStatistics( REGION_NAME );

			assertEquals( 0, cacheStatistics.getMissCount() );
			assertEquals( 1, cacheStatistics.getHitCount() );
			assertEquals( 0, cacheStatistics.getPutCount() );

			stats.clear();

			assertFalse( Hibernate.isInitialized( anEntity1.values ) );
			Hibernate.initialize( anEntity1.values );

			cacheStatistics = stats.getSecondLevelCacheStatistics( REGION_NAME );
			assertEquals( 0, cacheStatistics.getMissCount() );
			assertEquals( 1, cacheStatistics.getHitCount() );
			assertEquals( 0, cacheStatistics.getPutCount() );

			assertEquals( anEntity.values, anEntity1.values );
			stats.clear();

			AnotherEntity anotherEntity1 = (AnotherEntity) s.get( AnotherEntity.class, anotherEntity.id );

			cacheStatistics = stats.getSecondLevelCacheStatistics( REGION_NAME );
			assertEquals( 0, cacheStatistics.getMissCount() );
			assertEquals( 1, cacheStatistics.getHitCount() );
			assertEquals( 0, cacheStatistics.getPutCount() );

			stats.clear();

			assertFalse( Hibernate.isInitialized( anotherEntity1.values ) );
			Hibernate.initialize( anotherEntity1.values );

			cacheStatistics = stats.getSecondLevelCacheStatistics( REGION_NAME );
			assertEquals( 0, cacheStatistics.getMissCount() );
			assertEquals( 1, cacheStatistics.getHitCount() );
			assertEquals( 0, cacheStatistics.getPutCount() );

			assertEquals( anotherEntity.values, anotherEntity1.values );

			s.getTransaction().commit();
			s.close();
		}
		catch (Exception e) {
			setRollbackOnlyTx( e );
		}
		finally {
			commitOrRollbackTx();
		}
	}

	@Entity(name = "AnEntity")
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = REGION_NAME)
	public static class AnEntity {
		@Id
		private int id;

		@ElementCollection
		@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = REGION_NAME)
		private Set<String> values = new HashSet<String>();
	}

	@Entity(name = "AnotherEntity")
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = REGION_NAME)
	public static class AnotherEntity {
		@Id
		private int id;

		@ElementCollection
		@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = REGION_NAME)
		private Set<Integer> values = new HashSet<Integer>();
	}

}
