/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 2011/10/20 Unit test for code added in MergeContext for performance improvement.
 *
 * @author Wim Ockerman @ CISCO
 */
public class MergeOperationContextTest extends BaseCoreFunctionalTestCase {
	private EventSource session = null;

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
		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		final MergeEvent mergeEvent = new MergeEvent( mergeEntity, session );
		cache.beforeOperation( mergeEvent );

        Object managedEntity = new Simple( 2 );
        
        cache.addMergeData( mergeEntity, managedEntity );

		checkCacheConsistency( cache, 1 );

		Assert.assertNotNull( cache.getEntityCopyFromMergeEntity( mergeEntity ) );
        Assert.assertNull( cache.getEntityCopyFromMergeEntity( managedEntity ) );

		Assert.assertTrue( cache.getMergeEntityFromEntityCopy( managedEntity ) != null );
		Assert.assertTrue( cache.getMergeEntityFromEntityCopy( mergeEntity ) == null );
		Assert.assertTrue( cache.getMergeEntityFromEntityCopy( managedEntity ) == mergeEntity );

		cache.afterOperation( mergeEvent, true );

		assertFalse( cache.isInProgress() );
	}

	@Test
    public void testMergeToManagedEntityFillFollowedByInvert() {

        Object mergeEntity = new Simple( 1 );
		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		cache.beforeOperation( new MergeEvent( mergeEntity, session ) );

		Object managedEntity = new Simple( 2 );
        
        cache.addMergeData( mergeEntity, managedEntity );

		checkCacheConsistency( cache, 1 );

		Assert.assertNotNull( cache.getEntityCopyFromMergeEntity( mergeEntity ) );
        Assert.assertNull( cache.getEntityCopyFromMergeEntity( managedEntity ) );
        
        Assert.assertTrue( cache.getMergeEntityFromEntityCopy( managedEntity ) != null );
        assertTrue( cache.getMergeEntityFromEntityCopy( mergeEntity ) == null );
    }

	@Test
    public void testMergeToManagedEntityFillFollowedByInvertUsingPutWithSetOperatedOnArg() {

        Object mergeEntity = new Simple( 1 );

		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		final MergeEvent mergeEvent = new MergeEvent( mergeEntity, session );
		cache.beforeOperation( mergeEvent );

        Object managedEntity = new Simple( 2 );
        
        cache.addMergeData( mergeEntity, managedEntity );

		checkCacheConsistency( cache, 1 );

		Assert.assertNotNull( cache.getEntityCopyFromMergeEntity( mergeEntity ) );
        Assert.assertNull( cache.getEntityCopyFromMergeEntity( managedEntity ) );

        Assert.assertTrue( cache.getMergeEntityFromEntityCopy( managedEntity ) != null );
        Assert.assertTrue( cache.getMergeEntityFromEntityCopy( mergeEntity ) == null );
        
        cache.afterOperation( mergeEvent, true );

		cache.beforeOperation( mergeEvent );

		checkCacheConsistency( cache, 0 );

		cache.addMergeDataBeforeInMergeProcess( mergeEntity, managedEntity );
		assertFalse( cache.isInMergeProcess( mergeEntity ) );
		cache.markMergeDataInMergeProcess( mergeEntity );

		checkCacheConsistency( cache, 1 );

		Assert.assertNotNull( cache.getEntityCopyFromMergeEntity( mergeEntity ) );
        Assert.assertNull( cache.getEntityCopyFromMergeEntity( managedEntity ) );
    }

	@Test
	public void testReplaceManagedEntity() {

		Simple mergeEntity = new Simple( 1 );
		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		cache.beforeOperation( new MergeEvent( mergeEntity, session ) );

		Simple managedEntity = new Simple( 0 );
		cache.addMergeData( mergeEntity, managedEntity );

		Simple managedEntityNew = new Simple( 0 );
		try {
			cache.addMergeData( mergeEntity, managedEntityNew );
		}
		catch( IllegalArgumentException ex) {
			// expected; cannot replace the managed entity result for a particular merge entity.
		}
	}

	@Test
	public void testManagedEntityAssociatedWithNewAndExistingMergeEntities() {

		Simple mergeEntity = new Simple( 1 );
		Simple managedEntity = new Simple( 0 );

		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		cache.beforeOperation( new MergeEvent( managedEntity, session ) );

		cache.addMergeData( mergeEntity, managedEntity );
		cache.addMergeData( new Simple( 1 ), managedEntity );
	}

	@Test
	public void testManagedAssociatedWith2ExistingMergeEntities() {

		Simple mergeEntity1 = new Simple( 1 );
		Simple managedEntity1 = new Simple( 1 );
		Simple managedEntity2 = new Simple( 2 );

		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		cache.beforeOperation( new MergeEvent( mergeEntity1, session ) );

		cache.addMergeData( mergeEntity1, managedEntity1 );

		try {
			cache.addMergeData( mergeEntity1, managedEntity2 );
			Assert.fail( "should have thrown IllegalArgumentException" );
		}
		catch( IllegalArgumentException ex ) {
			// expected; cannot change managed entity associated with a merge entity
		}
	}

	private void checkCacheConsistency(MergeOperationContextImpl cache, int expectedSize) {
		// TODO: FIX THIS!!!!
		/*
		Set entrySet = cache.entrySet();
		Set cacheKeys = cache.keySet();
		Collection cacheValues = cache.values();
		Map invertedMap = cache.invertMap();

		Assert.assertEquals( expectedSize, entrySet.size() );
		Assert.assertEquals( expectedSize, cache.size() );
		Assert.assertEquals( expectedSize, invertedMap.size() );

		for ( Object entry : cache.entrySet() ) {
			Map.Entry mapEntry = ( Map.Entry ) entry;
			Assert.assertSame( cache.getMergeDataFromMergeEntity( mapEntry.getKey() ), mapEntry.getValue() );
			Assert.assertTrue( cacheKeys.contains( mapEntry.getKey() ) );
			Assert.assertTrue( cacheValues.contains( mapEntry.getValue() ) );
			Assert.assertSame( mapEntry.getKey(), invertedMap.get( mapEntry.getValue() ) );
		}
		*/
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
