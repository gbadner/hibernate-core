/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.operationContext.spi.EntityStatus;
import org.hibernate.engine.operationContext.spi.MergeData;
import org.hibernate.engine.operationContext.spi.MergeOperationContext;
import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.EntityCopyNotAllowedObserver;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.service.ServiceRegistry;

/**
 * Implementation of {@link MergeOperationContext}.
 *
 * @author Gail Badner
 */
public class MergeOperationContextImpl extends AbstractSaveOperationContextImpl<MergeEvent>
		implements org.hibernate.engine.operationContext.spi.MergeOperationContext {
	private static final Logger LOG = Logger.getLogger( MergeOperationContextImpl.class );

	private EntityCopyObserver entityCopyObserver;

	private Map<Object,MergeData> mergeEntityToMergeDataXref = new IdentityHashMap<Object,MergeData>(10);
		// key is an merge entity (passed to Session#merge);
		// value is the MergeData containing the associated entity copy.
		// (entity copy is ultimately managed in the persistence context).

	private Map<Object,Object> entityCopyToMergeEntityXref = new IdentityHashMap<Object,Object>( 10 );
		// for performance reasons, maintained to enable look ups by entity copy.
		// key is the entity copy;
		// value is the merge entity; if multiple representations of the same persistent entity
		// are added to this MergeOperationContextImpl, then the value will contain the most
		// recently added merge entity that is associated with the entity copy.

	MergeOperationContextImpl() {
		super( MergeEvent.class );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.MERGE;
	}

	@Override
	protected void doBeforeOperation() {
		if ( entityCopyObserver == null ) {
			entityCopyObserver = createEntityCopyObserver( getOperationContextData().getSession().getFactory() );
		}
		super.doBeforeOperation();
	}

	@Override
	protected void doAfterSuccessfulOperation() {
		entityCopyObserver.topLevelMergeComplete( getSession() );
		super.doAfterSuccessfulOperation();
	}

	private static EntityCopyObserver createEntityCopyObserver(SessionFactoryImplementor sessionFactory) {
		final ServiceRegistry serviceRegistry = sessionFactory.getServiceRegistry();
		final ConfigurationService configurationService
				= serviceRegistry.getService( ConfigurationService.class );
		String entityCopyObserverStrategy = configurationService.getSetting(
				"hibernate.event.merge.entity_copy_observer",
				new ConfigurationService.Converter<String>() {
					@Override
					public String convert(Object value) {
						return value.toString();
					}
				},
				EntityCopyNotAllowedObserver.SHORT_NAME
		);
		LOG.debugf( "EntityCopyObserver strategy: %s", entityCopyObserverStrategy );
		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
		return strategySelector.resolveStrategy( EntityCopyObserver.class, entityCopyObserverStrategy );
	}

	@Override
	public void clear() {
		mergeEntityToMergeDataXref.clear();
		entityCopyToMergeEntityXref.clear();
		entityCopyObserver.clear();
		super.clear();
	}

	@Override
	public Object getEntityCopyFromMergeEntity(Object mergeEntity) {
		checkIsValid();
		if ( mergeEntity == null ) {
			throw new IllegalArgumentException( "mergeEntity must be non-null" );
		}
		final MergeData mergeData = mergeEntityToMergeDataXref.get( mergeEntity );
		return mergeData == null ? null : mergeData.getEntityCopy();
	}

	@Override
	public Object getMergeEntityFromEntityCopy(Object entityCopy) {
		checkIsValid();
		if ( entityCopy == null ) {
			throw new IllegalArgumentException( "entityCopy must be non-null" );
		}
		return entityCopyToMergeEntityXref.get( entityCopy );
	}

	@Override
	public EntityStatus getMergeEntityStatus(Object mergeEntity) {
		checkIsValid();
		if ( mergeEntity == null ) {
			throw new IllegalArgumentException( "mergeEntity must be non-null." );
		}
		final MergeDataImpl mergeData = (MergeDataImpl) mergeEntityToMergeDataXref.get( mergeEntity );
		if ( mergeData == null ) {
			throw new IllegalArgumentException( "mergeEntity has not been added as a merge entity" );
		}
		return mergeData.getMergeEntityStatus();
	}

	@Override
	public boolean addMergeData(EntityStatus mergeEntityStatus, Object mergeEntity, Object entityCopy) {
		checkIsValid();
		if ( mergeEntityStatus == null || mergeEntity == null || entityCopy == null ) {
			throw new IllegalArgumentException( "mergeEntityStatus, mergeEntity, and entityCopy must be non-null" );
		}
		final MergeDataImpl newMergeData = new MergeDataImpl( mergeEntityStatus, mergeEntity, entityCopy, true );
		final MergeDataImpl oldMergeData = (MergeDataImpl) mergeEntityToMergeDataXref.put( mergeEntity, newMergeData );

		// If entityCopy already corresponds with a different merge entity, that means
		// that there are multiple entities being merged that correspond with the same entity copy.
		// In the following, the old merge entity will be replaced with mergeEntity in entityCopyToMergeEntityXref.
		final Object oldMergeEntityFromEntityCopy = entityCopyToMergeEntityXref.put( entityCopy, mergeEntity );

		if ( oldMergeData == null ) {
			// this is a new mapping for mergeEntity in mergeEntityToMergeDataXref
			if  ( oldMergeEntityFromEntityCopy != null ) {
				if ( mergeEntity == oldMergeEntityFromEntityCopy ) {
					throw new IllegalStateException(
							"mergeEntityToMergeDataXref and entityCopyToMergeEntityXref are out of sync."
					);
				}
				// oldMergeEntityFromEntityCopy was a different merge entity with the same corresponding entity copy;
				entityCopyObserver.entityCopyDetected(
						entityCopy,
						mergeEntity,
						oldMergeEntityFromEntityCopy,
						getSession()
				);
			}
		}
		else {
			// mergeEntity was already mapped in mergeEntityToMergeDataXref
			if ( oldMergeData.getEntityCopy() != entityCopy ) {
				throw new IllegalStateException(
						"Error occurred while storing a merge Entity " + printEntity( mergeEntity )
								+ ". It was previously associated with entity copy" + printEntity( oldMergeData.getEntityCopy() )
								+ ". Attempted to replace entity copy with " + printEntity( entityCopy )
				);
			}
			if ( oldMergeData.getMergeEntityStatus() != mergeEntityStatus ) {
				throw new IllegalStateException( "" +
						String.format(
								"Merge entity status was already set to [%s]; attempt to change it to [%s].",
								oldMergeData.getMergeEntityStatus(),
								mergeEntityStatus
						)
				);
			}
			if ( !oldMergeData.isInMergeProcess() ) {
				throw new IllegalStateException(
						"The merge entity was already added using addTransientMergeDataBeforeInMergeProcess();" +
								"use markTransientMergeDataInMergeProcess() to indicate it is now in the merge process."
				);
			}
		}

		return oldMergeData == null;
	}

	@Override
	public void addTransientMergeDataBeforeInMergeProcess(Object mergeEntity, Object entityCopy) {
		checkIsValid();
		if ( mergeEntity == null || entityCopy == null ) {
			throw new IllegalArgumentException( "mergeEntity and entityCopy must be non-null." );
		}
		final MergeDataImpl transientMergeData = new MergeDataImpl( EntityStatus.TRANSIENT, mergeEntity, entityCopy, false );
		if ( mergeEntityToMergeDataXref.put( mergeEntity, transientMergeData ) != null ) {
			throw new IllegalStateException( "mergeEntityToMergeDataXref already contained data for mergeData." );
		}
		if ( entityCopyToMergeEntityXref.put( entityCopy, mergeEntity ) != null ) {
			throw new IllegalStateException( "entityCopyToMergeEntityXref already contained data for entityCopy" );
		}
	}

	@Override
	public boolean isInMergeProcess(Object mergeEntity) {
		checkIsValid();
		if ( mergeEntity == null ) {
			throw new IllegalArgumentException( "mergeEntity must be non-null." );
		}
		final MergeDataImpl mergeDataImpl = (MergeDataImpl) mergeEntityToMergeDataXref.get( mergeEntity );
		return mergeDataImpl != null && mergeDataImpl.isInMergeProcess();
	}

	@Override
	public void markTransientMergeDataInMergeProcess(Object mergeEntity) {
		checkIsValid();
		if ( mergeEntity == null ) {
			throw new IllegalArgumentException( "mergeEntity must be non-null." );
		}
		final MergeDataImpl mergeDataImpl = (MergeDataImpl) mergeEntityToMergeDataXref.get( mergeEntity);
		if ( mergeDataImpl == null ) {
			throw new IllegalStateException(
					"mergeEntityToMergeDataXref does not contain mergeEntity."
			);
		}
		if ( mergeDataImpl.isInMergeProcess() ) {
			throw new IllegalStateException( "mergeEntity was already in the merge process." );
		}
		if ( mergeDataImpl.getMergeEntityStatus() != EntityStatus.TRANSIENT ) {
			throw new IllegalStateException(
					"mergeEntity status is not 'transient'; it was " + mergeDataImpl.getMergeEntityStatus().name()
			);
		}
		mergeDataImpl.markInMergeProcess();
	}

	@Override
	public Collection<MergeData> getAllMergeData() {
		checkIsValid();
		return Collections.unmodifiableCollection( mergeEntityToMergeDataXref.values() );
	}

	private String printEntity(Object entity) {
		if ( getSession().getPersistenceContext().getEntry( entity ) != null ) {
			return MessageHelper.infoString( getSession().getEntityName( entity ), getSession().getIdentifier( entity ) );
		}
		return "[" + entity + "]";
	}
}
