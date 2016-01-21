/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import java.util.Collection;
import java.util.Iterator;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.operationContext.spi.EntityStatus;
import org.hibernate.engine.operationContext.spi.MergeData;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 2011/10/20 Unit test for code added in MergeContext for performance improvement.
 *
 * @author Wim Ockerman @ CISCO
 * @author Gail Badner
 */
public class MergeOperationContextImplTest extends BaseCoreFunctionalTestCase {
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
	public void testMergeOperationContextNotInProgress() {
		final MergeOperationContextImpl cache = new MergeOperationContextImpl();
		assertFalse( cache.isInProgress() );
		final Object entity1 = new Simple( 1 );
		final Object entity2 = new Simple( 2 );
		try {
			cache.isInMergeProcess( entity1 );
			fail( "should have failed because MergeOperationContext is not in progress. ");
		}
		catch( IllegalStateException ex ) {
			// expected
		}
		try {
			cache.getEntityCopyFromMergeEntity( entity1 );
			fail( "should have failed because MergeOperationContext is not in progress. ");
		}
		catch( IllegalStateException ex) {
			// expected
		}
		try {
			cache.getMergeEntityFromEntityCopy( entity1 );
			fail( "should have failed because MergeOperationContext is not in progress. ");
		}
		catch( IllegalStateException ex ) {
			// expected
		}
		try {
			cache.getMergeEntityStatus( entity1 );
			fail( "should have failed because MergeOperationContext is not in progress. ");
		}
		catch( IllegalStateException ex ) {
			// expected
		}
		try {
			cache.addMergeData( EntityStatus.PERSISTENT, entity1, entity1 );
			fail( "should have failed because MergeOperationContext is not in progress. ");
		}
		catch( IllegalStateException ex ) {
			// expected
		}
		try {
			cache.addTransientMergeDataBeforeInMergeProcess( entity1, entity2 );
			fail( "should have failed because MergeOperationContext is not in progress. ");
		}
		catch( IllegalStateException ex ) {
			// expected
		}
		try {
			cache.markTransientMergeDataInMergeProcess( entity1 );
			fail( "should have failed because MergeOperationContext is not in progress. ");
		}
		catch( IllegalStateException ex ) {
			// expected
		}
	}

	@Test
	public void testBeforeOperation() {
		final MergeOperationContextImpl cache = new MergeOperationContextImpl();

		assertFalse( cache.isInProgress() );

		final Object entity1 = new Simple( 1 );
		final MergeEvent mergeEvent = new MergeEvent( entity1, session );

		cache.beforeOperation( mergeEvent );

		assertTrue( cache.isInProgress() );

		try {
			// try to start operation again
			cache.beforeOperation( mergeEvent );
			fail( "should have failed because MergeOperationContext is already in progress. ");
		}
		catch( IllegalStateException ex ) {
			// expected
		}
	}

	@Test
	public void testAfterOperation() {
		final MergeOperationContextImpl cache = new MergeOperationContextImpl();

		assertFalse( cache.isInProgress() );

		final Object entity1 = new Simple( 1 );
		final MergeEvent mergeEvent = new MergeEvent( entity1, session );

		try {
			// call afterOperation() before beforeOperation() was called
			cache.afterOperation( mergeEvent, true );
			fail( "should have failed because MergeOperationContext is already in progress. " );
		}
		catch( IllegalStateException ex ) {
			// expected
		}

		assertFalse( cache.isInProgress() );
		cache.beforeOperation( mergeEvent );
		assertTrue( cache.isInProgress() );
		cache.afterOperation( mergeEvent, true );
		assertFalse( cache.isInProgress() );

		try {
			// call afterOperation() again
			cache.afterOperation( mergeEvent, true );
			fail( "should have failed because MergeOperationContext is no longer in progress. " );
		}
		catch( IllegalStateException ex ) {
			// expected
		}

		// call beforeOperation and afterOperation with different MergeEvent objects.
		cache.beforeOperation( mergeEvent );
		final Object entity2 = new Simple( 2 );
		try {
			cache.afterOperation( new MergeEvent( entity2, session ), true );
			fail( "should have failed because a different MergeEvent was provided to afterOperation(). " );
		}
		catch( IllegalStateException ex ) {
			// expected
		}
	}

	@Test
	public void testIsInMergeProcess() {
		final MergeOperationContextImpl cache = new MergeOperationContextImpl();

		final Object entity1 = new Simple( 1 );
		final Object entity1Copy = new Simple( 1 );
		final Object entity2 = new Simple( 2 );
		final Object entity2Copy = new Simple( 2 );

		final MergeEvent mergeEvent = new MergeEvent( entity1, session );

		cache.beforeOperation( mergeEvent );

		// test w/ an entity that has not been added using addMergeData or addTransientMergeDataBeforeInMergeProcess
		assertFalse( cache.isInMergeProcess( entity1 ) );

		// test w/ an entity added using addMergeData()
		cache.addMergeData( EntityStatus.DETACHED, entity1, entity1Copy );
		assertTrue( cache.isInMergeProcess( entity1 ) );
		// only a merge entity is considered to be in the merge process;
		assertFalse( cache.isInMergeProcess( entity1Copy ) );

		// test w/ an entity added using addTransientMergeDataBeforeInMergeProcess
		cache.addTransientMergeDataBeforeInMergeProcess( entity2, entity2Copy );
		// entities added using addTransientMergeDataBeforeInMergeProcess
		// are not considered to be in the merge process.
		assertFalse( cache.isInMergeProcess( entity2 ) );
		assertFalse( cache.isInMergeProcess( entity2Copy ) );

		// test after adding to merge process via markTransientMergeDataInMergeProcess.
		cache.markTransientMergeDataInMergeProcess( entity2 );
		assertTrue( cache.isInMergeProcess( entity2 ) );
		assertFalse( cache.isInMergeProcess( entity2Copy ) );

		cache.afterOperation( mergeEvent, true );

		try {
			cache.isInMergeProcess( entity1 );
			fail( "should have failed because operation should no longer be in progress." );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testMergeEntityAndEntityCopyXRefs() {
		final MergeOperationContextImpl cache = new MergeOperationContextImpl();

		final Object entity1 = new Simple( 1 );
		final Object entity1Copy = new Simple( 1 );
		final Object entity2 = new Simple( 2 );
		final Object entity2Copy = new Simple( 2 );

		final MergeEvent mergeEvent = new MergeEvent( entity1, session );

		cache.beforeOperation( mergeEvent );

		// check before adding the data
		assertNull( cache.getEntityCopyFromMergeEntity( entity1 ) );
		assertNull( cache.getEntityCopyFromMergeEntity( entity2 ) );
		assertNull( cache.getEntityCopyFromMergeEntity( entity1Copy ) );
		assertNull( cache.getEntityCopyFromMergeEntity( entity2Copy ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity1 ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity2 ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity1Copy ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity2Copy ) );

		// add one merge entity / entity copy w/ addMergeData
		cache.addMergeData( EntityStatus.DETACHED, entity1, entity1Copy );

		assertSame( entity1, cache.getMergeEntityFromEntityCopy( entity1Copy ) );
		assertSame( entity1Copy, cache.getEntityCopyFromMergeEntity( entity1 ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity1 ) );
		assertNull( cache.getEntityCopyFromMergeEntity( entity1Copy ) );

		// add a different merge entity / entity copy w/ addTransientMergeDataBeforeInMergeProcess
		cache.addTransientMergeDataBeforeInMergeProcess( entity2, entity2Copy );

		assertSame( entity2, cache.getMergeEntityFromEntityCopy( entity2Copy ) );
		assertSame( entity2Copy, cache.getEntityCopyFromMergeEntity( entity2 ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity2 ) );
		assertNull( cache.getEntityCopyFromMergeEntity( entity2Copy ) );

		// check that cross-references are gone after calling afterOperation
		cache.afterOperation( mergeEvent, true );

		try {
			cache.getEntityCopyFromMergeEntity( entity1 );
			fail( "should have failed because operation should no longer be in progress." );
		}
		catch (IllegalStateException ex) {
			// expected
		}

		try {
			cache.getMergeEntityFromEntityCopy( entity1Copy );
			fail( "should have failed because operation should no longer be in progress." );
		}
		catch (IllegalStateException ex) {
			// expected
		}

		// make sure the data is gone.
		cache.beforeOperation( mergeEvent );

		// check before adding the data
		assertNull( cache.getEntityCopyFromMergeEntity( entity1 ) );
		assertNull( cache.getEntityCopyFromMergeEntity( entity2 ) );
		assertNull( cache.getEntityCopyFromMergeEntity( entity1Copy ) );
		assertNull( cache.getEntityCopyFromMergeEntity( entity2Copy ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity1 ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity2 ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity1Copy ) );
		assertNull( cache.getMergeEntityFromEntityCopy( entity2Copy ) );
	}

	@Test
	public void testMergeEntityStatus() {
		final MergeOperationContextImpl cache = new MergeOperationContextImpl();

		final Object entity1 = new Simple( 1 );
		final Object entity1Copy = new Simple( 1 );
		final Object entity2 = new Simple( 2 );
		final Object entity2Copy = new Simple( 2 );

		final MergeEvent mergeEvent = new MergeEvent( entity1, session );

		cache.beforeOperation( mergeEvent );

		// add one merge entity / entity copy w/ addMergeData
		cache.addMergeData( EntityStatus.DETACHED, entity1, entity1Copy );

		assertEquals( EntityStatus.DETACHED, cache.getMergeEntityStatus( entity1 ) );
		try {
			cache.getMergeEntityStatus( entity1Copy );
			fail( "should have failed because entity1Copy is not a merge entity." );
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		// add a different merge entity / entity copy w/ addTransientMergeDataBeforeInMergeProcess
		cache.addTransientMergeDataBeforeInMergeProcess( entity2, entity2Copy );
		assertEquals( EntityStatus.TRANSIENT, cache.getMergeEntityStatus( entity2 ) );
		try {
			cache.getMergeEntityStatus( entity2Copy );
			fail( "should have failed because entity2Copy is not a merge entity." );
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		cache.markTransientMergeDataInMergeProcess( entity2 );
		assertEquals( EntityStatus.TRANSIENT, cache.getMergeEntityStatus( entity2 ) );
		assertTrue( cache.isInMergeProcess( entity2 ) );

		cache.afterOperation( mergeEvent, true );
		try {
			cache.getMergeEntityStatus( entity1 );
			fail( "should have failed because operation should no longer be in progress." );
		}
		catch (IllegalStateException ex) {
			// expected
		}

		// make sure the data is gone.
		cache.beforeOperation( mergeEvent );

		try {
			cache.getMergeEntityStatus( entity1 );
			fail( "should have failed because entity1 should no longer be in cross-reference." );
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

	}

	@Test
	public void testAddMergeData() {
		final MergeOperationContextImpl cache = new MergeOperationContextImpl();

		final Object entity1 = new Simple( 1 );
		final Object entityCopy = new Simple( 1 );

		final MergeEvent mergeEvent = new MergeEvent( entity1, session );

		cache.beforeOperation( mergeEvent );

		// add merge entity / entity copy that is not in cache yet.
		assertTrue( cache.addMergeData( EntityStatus.DETACHED, entity1, entityCopy ) );
		assertTrue( cache.isInMergeProcess( entity1 ) );
		assertSame( entityCopy, cache.getEntityCopyFromMergeEntity( entity1 ) );
		assertSame( entity1, cache.getMergeEntityFromEntityCopy( entityCopy ) );

		// add the same merge entity / entity copy again
		assertFalse( cache.addMergeData( EntityStatus.DETACHED, entity1, entityCopy ) );
		assertTrue( cache.isInMergeProcess( entity1 ) );
		assertSame( entityCopy, cache.getEntityCopyFromMergeEntity( entity1 ) );
		assertSame( entity1, cache.getMergeEntityFromEntityCopy( entityCopy ) );

		// add a different merge entity with same entity copy
		final Object entity1A = new Simple( 2 );
		assertTrue( cache.addMergeData( EntityStatus.DETACHED, entity1A, entityCopy ) );
		assertTrue( cache.isInMergeProcess( entity1A ) );
		assertSame( entityCopy, cache.getEntityCopyFromMergeEntity( entity1A ) );
		// cache.getMergeEntityFromEntityCopy( entityCopy ) will return entity1A,
		// because entity1 has been displaced in entity copy / merge entity
		// cross-reference.
		assertSame( entity1A, cache.getMergeEntityFromEntityCopy( entityCopy ) );
		// check that entity1 is still in merge process
		assertTrue( cache.isInMergeProcess( entity1 ) );
		// check that entity1 still has same entity copy
		assertSame( entityCopy, cache.getEntityCopyFromMergeEntity( entity1 ) );
	}

	@Test
	public void testAddMergeDataTwiceDetachedAndPersistent() {
		final MergeOperationContextImpl cache = new MergeOperationContextImpl();

		final Object persistentEntity = new Simple( 1 );
		final Object detachedEntity = new Simple( 1 );

		final MergeEvent mergeEvent = new MergeEvent( persistentEntity, session );

		cache.beforeOperation( mergeEvent );

		// add a merge entity that is PERSISTENT (merge entity is same as entity copy
		assertTrue( cache.addMergeData( EntityStatus.PERSISTENT, persistentEntity, persistentEntity ) );
		assertTrue( cache.isInMergeProcess( persistentEntity ) );
		assertSame( persistentEntity, cache.getMergeEntityFromEntityCopy( persistentEntity ) );
		assertSame( persistentEntity, cache.getEntityCopyFromMergeEntity( persistentEntity ) );

		// add a merge entity that is DETACHED (entity copy will be persistentEntity)
		assertTrue( cache.addMergeData( EntityStatus.DETACHED, detachedEntity, persistentEntity ) );
		assertTrue( cache.isInMergeProcess( detachedEntity ) );
		assertSame( persistentEntity, cache.getEntityCopyFromMergeEntity( detachedEntity ) );
		// cache.getMergeEntityFromEntityCopy( persistentEntity ) will return detachedEntity,
		// because persistentEntity has been displaced in entity copy / merge entity
		// cross-reference.
		assertSame( detachedEntity, cache.getMergeEntityFromEntityCopy( persistentEntity ) );
		// check that persistentEntity is still in merge process
		assertTrue( cache.isInMergeProcess( persistentEntity ) );
		// check that persistentEntity still has same entity copy
		assertSame( persistentEntity, cache.getEntityCopyFromMergeEntity( persistentEntity ) );
	}

 	@Test
	public void testAddMergeDataReplaceManagedEntity() {
		Simple mergeEntity = new Simple( 1 );
		Simple entityCopy1 = new Simple( 1 );
		Simple entityCopy2 = new Simple( 1 );

		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		cache.beforeOperation( new MergeEvent( mergeEntity, session ) );

		cache.addMergeData( EntityStatus.DETACHED, mergeEntity, entityCopy1 );

		try {
			cache.addMergeData( EntityStatus.DETACHED, mergeEntity, entityCopy2 );
			fail( "should have failed because entity copy was replaced." );
		}
		catch( IllegalStateException ex) {
			// expected; cannot replace the managed entity result for a particular merge entity.
		}
	}

	@Test
	public void testAddMergeDataTwiceDiffEntityStatus() {
		Simple mergeEntity = new Simple( 1 );
		Simple entityCopy = new Simple( 1 );

		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		cache.beforeOperation( new MergeEvent( mergeEntity, session ) );

		cache.addMergeData( EntityStatus.TRANSIENT, mergeEntity, entityCopy );

		try {
			cache.addMergeData( EntityStatus.DETACHED, mergeEntity, entityCopy );
			fail( "should have failed because entity status was changed for the same merge entity." );
		}
		catch( IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testTransientMergeDataBeforeInMergeProcess() {
		Simple mergeEntity = new Simple( 1 );
		Simple entityCopy = new Simple( 1 );

		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		cache.beforeOperation( new MergeEvent( mergeEntity, session ) );

		cache.addTransientMergeDataBeforeInMergeProcess( mergeEntity, entityCopy );
		assertFalse( cache.isInMergeProcess( mergeEntity ) );
		assertEquals( EntityStatus.TRANSIENT, cache.getMergeEntityStatus( mergeEntity ) );
		assertSame( entityCopy, cache.getEntityCopyFromMergeEntity( mergeEntity ) );
		assertSame( mergeEntity, cache.getMergeEntityFromEntityCopy( entityCopy ) );

		// try adding again; it should fail
		try {
			cache.addTransientMergeDataBeforeInMergeProcess( mergeEntity, entityCopy );
			fail( "should have failed because mergeEntity was already added" );
		}
		catch (IllegalStateException ex) {
			// expected
		}

		cache.markTransientMergeDataInMergeProcess( mergeEntity );
		assertTrue( cache.isInMergeProcess( mergeEntity ) );

		assertEquals( EntityStatus.TRANSIENT, cache.getMergeEntityStatus( mergeEntity ) );
		assertSame( entityCopy, cache.getEntityCopyFromMergeEntity( mergeEntity ) );
		assertSame( mergeEntity, cache.getMergeEntityFromEntityCopy( entityCopy ) );

		// try marking it again; it should fail.
		try {
			cache.markTransientMergeDataInMergeProcess( mergeEntity );
			fail( "should have failed because mergeEntity was already marked" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testAddTransientDataBothWays() {
		Simple mergeEntity = new Simple( 1 );
		Simple entityCopy = new Simple( 1 );

		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		MergeEvent mergeEvent = new MergeEvent( mergeEntity, session );
		cache.beforeOperation( mergeEvent );

		cache.addTransientMergeDataBeforeInMergeProcess( mergeEntity, entityCopy );

		try {
			cache.addMergeData( EntityStatus.TRANSIENT, mergeEntity, entityCopy );
			fail( "should have failed because mergeEntity was already added, but is not in merge process yet.");
		}
		catch (IllegalStateException ex) {
			// expected
		}

		// cache is probably in an unstable state

		cache.afterOperation( mergeEvent, false );
		cache.beforeOperation( mergeEvent );

		cache.addTransientMergeDataBeforeInMergeProcess( mergeEntity, entityCopy );
		cache.markTransientMergeDataInMergeProcess( mergeEntity );
		// now that mergeEntity is marked as being in the merge process, it can be added again with addMergeData()
		assertFalse( cache.addMergeData( EntityStatus.TRANSIENT, mergeEntity, entityCopy ) );
	}

	@Test
	public void testAllMergeData() {
		Simple mergeEntity1A = new Simple( 1 );
		Simple mergeEntity1B = new Simple( 1 );
		Simple entityCopy1 = new Simple( 1 );
		Simple mergeEntity2 = new Simple( 2 );
		Simple entityCopy2 = new Simple( 2 );

		MergeOperationContextImpl cache = new MergeOperationContextImpl();
		MergeEvent mergeEvent = new MergeEvent( mergeEntity1A, session );
		cache.beforeOperation( mergeEvent );

		assertTrue( cache.getAllMergeData().isEmpty() );
		cache.addMergeData( EntityStatus.DETACHED, mergeEntity1A, entityCopy1 );
		assertEquals( 1, cache.getAllMergeData().size() );
		MergeData mergeData = cache.getAllMergeData().iterator().next();
		assertSame( mergeEntity1A, mergeData.getMergeEntity() );
		assertSame( entityCopy1, mergeData.getEntityCopy() );

		// add a different merge entity associated with the same entity copy;
		// the collection should contain a different MergeData for mergeEntity1A
		// and mergeEntity1B.
		cache.addMergeData( EntityStatus.DETACHED, mergeEntity1B, entityCopy1 );
		Collection<MergeData> allMergeData = cache.getAllMergeData();
		assertEquals( 2, allMergeData.size() );
		Iterator<MergeData> it = allMergeData.iterator();
		mergeData = it.next();
		if ( mergeData.getMergeEntity() == mergeEntity1A ) {
			assertSame( entityCopy1, mergeData.getEntityCopy() );
			mergeData = it.next();
			assertSame( mergeEntity1B, mergeData.getMergeEntity() );
			assertSame( entityCopy1, mergeData.getEntityCopy() );
		}
		else {
			assertSame( mergeEntity1B, mergeData.getMergeEntity() );
			assertSame( entityCopy1, mergeData.getEntityCopy() );
			mergeData = it.next();
			assertSame( mergeEntity1A, mergeData.getMergeEntity() );
			assertSame( entityCopy1, mergeData.getEntityCopy() );
		}

		// add another pair
		cache.addTransientMergeDataBeforeInMergeProcess( mergeEntity2, entityCopy2 );
		assertEquals( 3, allMergeData.size() );
		// find the MergeData for the last pair added.
		boolean found = false;
		for ( MergeData mergeDataLoop : cache.getAllMergeData() ) {
			if ( mergeDataLoop.getMergeEntity() == mergeEntity2 ) {
				found = true;
				assertSame( entityCopy2, mergeDataLoop.getEntityCopy() );
			}
		}
		assertTrue( found );
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
}
