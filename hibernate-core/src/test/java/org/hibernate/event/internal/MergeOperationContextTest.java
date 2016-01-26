/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 2011/10/20 Unit test for code added in MergeContext for performance improvement.
 *
 * @author Wim Ockerman @ CISCO
 */
public class MergeOperationContextTest extends BaseCoreFunctionalTestCase {
	private EventSource session = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Simple.class };
	}

	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty(
				"hibernate.event.merge.entity_copy_observer",
				DoNothingEntityCopyObserver.class.getName()
		);
	}

	@Before
	public void setUp() {
		session = (EventSource) openSession();
	}

	@After
	public void tearDown() {
		session.close();
		session = null;
	}

	@Test
    public void testMergeToManagedEntityFillFollowedByInvertMapping() {

        Object mergeEntity = new Simple( 1 );
		MergeOperationContext cache = new MergeOperationContext();
		cache.beforeOperation( new MergeEvent( mergeEntity, session ) );

        Object managedEntity = new Simple( 2 );
        
        cache.put(mergeEntity, managedEntity);

		checkCacheConsistency( cache, 1 );

		assertTrue( cache.containsMergeEntity( mergeEntity ) );
        assertFalse( cache.containsMergeEntity( managedEntity ) );
		assertTrue( cache.containsValue( managedEntity ) );

		assertTrue( cache.invertMap().containsKey( managedEntity ) );
        assertFalse( cache.invertMap().containsKey( mergeEntity ) );
		assertTrue( cache.invertMap().containsValue( mergeEntity ) );

		cache.clear();

		checkCacheConsistency( cache, 0 );

		assertFalse( cache.containsMergeEntity( mergeEntity ) );
        assertFalse(cache.invertMap().containsKey(managedEntity));
	}

	@Test
    public void testMergeToManagedEntityFillFollowedByInvert() {

        Object mergeEntity = new Simple( 1 );
		MergeOperationContext cache = new MergeOperationContext();
		cache.beforeOperation( new MergeEvent( mergeEntity, session ) );

		Object managedEntity = new Simple( 2 );
        
        cache.put( mergeEntity, managedEntity );

		checkCacheConsistency( cache, 1 );

		assertTrue( cache.containsMergeEntity( mergeEntity ) );
        assertFalse( cache.containsMergeEntity( managedEntity ) );
        
        assertTrue( cache.invertMap().containsKey( managedEntity ) );
        assertFalse( cache.invertMap().containsKey( mergeEntity ) );
    }

	@Test
    public void testMergeToManagedEntityFillFollowedByInvertUsingPutWithSetOperatedOnArg() {

        Object mergeEntity = new Simple( 1 );

		MergeOperationContext cache = new MergeOperationContext();
		cache.beforeOperation( new MergeEvent( mergeEntity, session ) );

        Object managedEntity = new Simple( 2 );
        
        cache.put(mergeEntity, managedEntity, true);

		checkCacheConsistency( cache, 1 );

		assertTrue(cache.containsMergeEntity( mergeEntity ));
        assertFalse( cache.containsMergeEntity( managedEntity ) );

        assertTrue( cache.invertMap().containsKey( managedEntity ) );
        assertFalse( cache.invertMap().containsKey( mergeEntity ) );
        
        cache.clear();

		checkCacheConsistency( cache, 0 );

		cache.put( mergeEntity, managedEntity, false );
		assertFalse( cache.isOperatedOn( mergeEntity ) );

		checkCacheConsistency( cache, 1 );

		assertTrue(cache.containsMergeEntity( mergeEntity ));
        assertFalse(cache.containsMergeEntity( managedEntity ));
    }

	@Test
	public void testReplaceManagedEntity() {

		Simple mergeEntity = new Simple( 1 );
		MergeOperationContext cache = new MergeOperationContext();
		cache.beforeOperation( new MergeEvent( mergeEntity, session ) );

		Simple managedEntity = new Simple( 0 );
		cache.put(mergeEntity, managedEntity);

		Simple managedEntityNew = new Simple( 0 );
		try {
			cache.put( mergeEntity, managedEntityNew );
		}
		catch( IllegalArgumentException ex) {
			// expected; cannot replace the managed entity result for a particular merge entity.
		}
	}

	@Test
	public void testManagedEntityAssociatedWithNewAndExistingMergeEntities() {

		Simple mergeEntity = new Simple( 1 );
		Simple managedEntity = new Simple( 0 );

		MergeOperationContext cache = new MergeOperationContext();
		cache.beforeOperation( new MergeEvent( managedEntity, session ) );

		cache.put(mergeEntity, managedEntity);
		cache.put( new Simple( 1 ), managedEntity );
	}

	@Test
	public void testManagedAssociatedWith2ExistingMergeEntities() {

		Simple mergeEntity1 = new Simple( 1 );
		Simple managedEntity1 = new Simple( 1 );
		Simple managedEntity2 = new Simple( 2 );

		MergeOperationContext cache = new MergeOperationContext();
		cache.beforeOperation( new MergeEvent( mergeEntity1, session ) );

		cache.put( mergeEntity1, managedEntity1 );

		try {
			cache.put( mergeEntity1, managedEntity2 );
			fail( "should have thrown IllegalArgumentException");
		}
		catch( IllegalArgumentException ex ) {
			// expected; cannot change managed entity associated with a merge entity
		}
	}

	private void checkCacheConsistency(MergeOperationContext cache, int expectedSize) {
		Set entrySet = cache.entrySet();
		Set cacheKeys = cache.keySet();
		Collection cacheValues = cache.values();
		Map invertedMap = cache.invertMap();

		assertEquals( expectedSize, entrySet.size() );
		assertEquals( expectedSize, cache.size() );
		assertEquals( expectedSize, invertedMap.size() );

		for ( Object entry : cache.entrySet() ) {
			Map.Entry mapEntry = ( Map.Entry ) entry;
			assertSame( cache.get( mapEntry.getKey() ), mapEntry.getValue() );
			assertTrue( cacheKeys.contains( mapEntry.getKey() ) );
			assertTrue( cacheValues.contains( mapEntry.getValue() ) );
			assertSame( mapEntry.getKey(), invertedMap.get( mapEntry.getValue() ) );
		}
	}

	@Entity
	private static class Simple {
		@Id
		private int value;

		public Simple(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "Simple{" +
					"value=" + value +
					'}';
		}
	}

	public static class DoNothingEntityCopyObserver implements EntityCopyObserver {

		@Override
		public void entityCopyDetected(Object managedEntity, Object mergeEntity1, Object mergeEntity2, EventSource session) {

		}

		@Override
		public void topLevelMergeComplete(EventSource session) {

		}

		@Override
		public void clear() {

		}
	}

	private class ExceptionThrowingEntityCopyObserver implements EntityCopyObserver {

		@Override
		public void entityCopyDetected(Object managedEntity, Object mergeEntity1, Object mergeEntity2, EventSource session) {
			throw new IllegalStateException( "Entity copies not allowed." );
		}

		@Override
		public void topLevelMergeComplete(EventSource session) {
		}

		@Override
		public void clear() {

		}
	}
}
