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
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.config.spi.ConfigurationService;
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
		// key is an entity to be merged;
		// value is the associated managed entity (result) in the persistence context.

	private Map<Object,MergeData> entityCopyToMergeDataXref = new IdentityHashMap<Object,MergeData>( 10 );
		// maintains the inverse of the mergeToManagedEntityXref for performance reasons.
		// key is the managed entity result in the persistence context.
		// value is the associated entity to be merged; if multiple
		// representations of the same persistent entity are added to the MergeContext,
		// value will be the most recently added merge entity that is
		// associated with the managed entity.

	MergeOperationContextImpl() {
		super( MergeEvent.class );
	}

	// key is a merge entity;
	// value is a flag indicating if the merge entity is currently in the merge process.

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.MERGE;
	}

	@Override
	public void doBeforeOperation() {
		if ( entityCopyObserver == null ) {
			entityCopyObserver = createEntityCopyObserver( getEvent().getSession().getFactory() );
		}
		super.doBeforeOperation();
	}

	@Override
	public void doAfterSuccessfulOperation() {
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

	/**
	 * Clears the MergeContext.
	 */
	public void clear() {
		mergeEntityToMergeDataXref.clear();
		entityCopyToMergeDataXref.clear();
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
		final MergeData mergeData = entityCopyToMergeDataXref.get( entityCopy );
		return mergeData == null ? null : mergeData.getMergeEntity();
	}

	@Override
	public boolean addMergeData(Object mergeEntity, Object entityCopy) {
		checkIsValid();
		if ( mergeEntity == null || entityCopy == null ) {
			throw new IllegalArgumentException( "null merge and managed entities are not supported by " + getClass().getName() );
		}

		MergeDataImpl oldMergeDataByMergeEntity = (MergeDataImpl) mergeEntityToMergeDataXref.get( mergeEntity );

		if ( oldMergeDataByMergeEntity == null ) {
			// this is a new mapping for mergeEntity in mergeEntityToMergeDataXref
			// put a new MergeData into mergeEntityToMergeDataXref
			final MergeDataImpl newMergeData = new MergeDataImpl( mergeEntity, entityCopy, true );
			mergeEntityToMergeDataXref.put( mergeEntity, newMergeData );

			// If entityCopy already corresponds with a different merge entity, that means
			// that there are multiple entities being merged that correspond with managedEntity.
			MergeData oldMergeDataByEntityCopy = entityCopyToMergeDataXref.get( entityCopy );
			if  ( oldMergeDataByEntityCopy != null ) {
				if ( mergeEntity == oldMergeDataByEntityCopy.getMergeEntity() ) {
					throw new IllegalStateException(
							"mergeEntityToMergeDataXref and entityCopyToMergeDataXref are out of sync."
					);
				}
				// oldMergeDataByEntityCopy was a different merge entity with the same corresponding entity copy;
				entityCopyObserver.entityCopyDetected(
						entityCopy,
						mergeEntity,
						oldMergeDataByEntityCopy.getMergeEntity(),
						getSession()
				);
			}

			// In the following, oldMergeDataByEntityCopy will be replaced with newMergeData in entityCopyToMergeDataXref.
			entityCopyToMergeDataXref.put( entityCopy, newMergeData );
		}
		else {
			// mergeEntity was already mapped in mergeToManagedEntityXref
			if ( oldMergeDataByMergeEntity.getEntityCopy() != entityCopy ) {
				throw new IllegalArgumentException(
						"Error occurred while storing a merge Entity " + printEntity( mergeEntity )
								+ ". It was previously associated with managed entity " + printEntity( oldMergeDataByMergeEntity.getEntityCopy() )
								+ ". Attempted to replace managed entity with " + printEntity( entityCopy )
				);
			}
			if ( !oldMergeDataByMergeEntity.isInMergeProcess() ) {
				oldMergeDataByMergeEntity.markInMergeProcess();
			}
		}

		return oldMergeDataByMergeEntity == null;
	}

	@Override
	public void addMergeDataBeforeInMergeProcess(Object mergeEntity, Object entityCopy) {
		if ( mergeEntity == null || entityCopy == null ) {
			throw new IllegalArgumentException( "mergeEntity and entityCopy must be non-null." );
		}
		final MergeDataImpl transientMergeData = new MergeDataImpl( mergeEntity, entityCopy, false );
		if ( mergeEntityToMergeDataXref.put( mergeEntity, transientMergeData ) != null ) {
			throw new IllegalStateException( "mergeEntityToMergeDataXref already contained data for mergeData." );
		}
		if ( entityCopyToMergeDataXref.put( entityCopy, transientMergeData ) != null ) {
			throw new IllegalStateException( "entityCopyToMergeDataXref already contained data for entityCopy" );
		}
	}

	@Override
	public boolean isInMergeProcess(Object mergeEntity) {
		if ( mergeEntity == null ) {
			throw new IllegalArgumentException( "mergeEntity must be non-null." );
		}
		final MergeDataImpl mergeDataImpl = (MergeDataImpl) mergeEntityToMergeDataXref.get( mergeEntity );
		return mergeDataImpl != null && mergeDataImpl.isInMergeProcess();
	}

	@Override
	public void markMergeDataInMergeProcess(Object mergeEntity) {
		if ( mergeEntity == null ) {
			throw new IllegalArgumentException( "mergeEntity must be non-null." );
		}
		final MergeDataImpl mergeDataImpl = (MergeDataImpl) mergeEntityToMergeDataXref.get( mergeEntity);
		if ( mergeDataImpl == null ) {
			throw new IllegalStateException(
					"mergeEntityToMergeDataXref does not contain mergeEntity."
			);
		}
		mergeDataImpl.markInMergeProcess();
	}

	@Override
	public Collection<MergeData> getAllMergeData() {
		return Collections.unmodifiableCollection( mergeEntityToMergeDataXref.values() );
	}

	private String printEntity(Object entity) {
		if ( getSession().getPersistenceContext().getEntry( entity ) != null ) {
			return MessageHelper.infoString( getSession().getEntityName( entity ), getSession().getIdentifier( entity ) );
		}
		// Entity was not found in current persistence context. Use Object#toString() method.
		return "[" + entity + "]";
	}
}
