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
import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.EntityCopyNotAllowedObserver;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.service.ServiceRegistry;

/**
 * MergeContext is a Map implementation that is intended to be used by a merge
 * event listener to keep track of each entity being merged and their corresponding
 * managed result. Entities to be merged may to be added to the MergeContext before
 * the merge operation has cascaded to that entity.
 *
 * "Merge entity" and "mergeEntity" method parameter refer to an entity that is (or will be)
 * merged via {@link org.hibernate.event.spi.EventSource#merge(Object mergeEntity)}.
 *
 * "Managed entity" and "managedEntity" method parameter refer to the managed entity that is
 * the result of merging an entity.
 *
 * A merge entity can be transient, detached, or managed. If it is managed, then it must be
 * the same as its associated entity result.
 *
 * If {@link #addMergeData(Object mergeEntity, Object managedEntity)} is called, and this
 * MergeContext already contains an entry with a different entity as the key, but
 * with the same (managedEntity) value, this means that multiple entity representations
 * for the same persistent entity are being merged. If this happens,
 * {@link org.hibernate.event.spi.EntityCopyObserver#entityCopyDetected(
 * Object managedEntity, Object mergeEntity1, Object mergeEntity2, org.hibernate.event.spi.EventSource)}
 * will be called. It is up to that method to determine the property course of
 * action for this situation.
 *
 * There are several restrictions.
 * <ul>
 *     <li>Methods that return collections (e.g., {@link #keySet()},
 *          {@link #values()}) return an
 *          unnmodifiable view of the collection;</li>
 *     <li>If {@link #addMergeData(Object mergeEntity, Object) managedEntity} or
 *         {@link #addMergeData(Object mergeEntity, Object managedEntity, boolean isOperatedOn)}
 *         is executed and this MergeMap already contains a cross-reference for
 *         <code>mergeEntity</code>, then <code>managedEntity</code> must be the
 *         same as what is already associated with <code>mergeEntity</code> in this
 *         MergeContext.
 *     </li>
 *      <li>the Map returned by {@link #invertMap()} will only contain the
 *          managed-to-merge entity cross-reference to its "newest"
 *          (most recently added) merge entity.</li>
 * </ul>
 * <p>
 * The following method is intended to be used by a merge event listener (and other
 * classes) in the same package to add a merge entity and its corresponding
 * managed entity to a MergeContext and indicate if the merge operation is
 * being performed on the merge entity yet.<p/>
 * {@link MergeOperationContextImpl#addMergeData(Object mergeEntity, Object managedEntity, boolean isOperatedOn)}
 * <p/>
 * The following method is intended to be used by a merge event listener (and other
 * classes) in the same package to indicate whether the merge operation is being
 * performed on a merge entity already in the MergeContext:
 * {@link MergeOperationContextImpl#setOperatedOn(Object mergeEntity, boolean isOperatedOn)
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

	/**
	 * Used only for testing.
	 */
	/** package-private */ boolean containsValue(Object managedEntity) {
		checkIsValid();
		if ( managedEntity == null ) {
			throw new NullPointerException( "null copies are not supported by " + getClass().getName() );
		}
		return entityCopyToMergeDataXref.containsKey( managedEntity );
	}

	/**
	 * Should only be used for testing.
	 *
	 * Returns an unmodifiable set view of the merge-to-managed entity cross-references contained in this MergeContext.
	 * @return an unmodifiable set view of the merge-to-managed entity cross-references contained in this MergeContext
	 *
	 * @see {@link Collections#unmodifiableSet(java.util.Set)}
	 *
	 */
	/* package-private */ Set entrySet() {
		checkIsValid();
		return Collections.unmodifiableSet( mergeEntityToMergeDataXref.entrySet() );
	}

	@Override
	public MergeData getMergeDataFromMergeEntity(Object mergeEntity) {
		checkIsValid();
		if ( mergeEntity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return mergeEntityToMergeDataXref.get( mergeEntity );
	}

	@Override
	public MergeData getMergeDataFromEntityCopy(Object entityCopy) {
		checkIsValid();
		if ( entityCopy == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return entityCopyToMergeDataXref.get( entityCopy );
	}

	@Override
	public boolean addMergeData(MergeData mergeData) {
		checkIsValid();
		if ( mergeData.getMergeEntity() == null || mergeData.getEntityCopy() == null ) {
			throw new NullPointerException( "null merge and managed entities are not supported by " + getClass().getName() );
		}

		MergeData oldMergeDataByMergeEntity = mergeEntityToMergeDataXref.put(  mergeData.getMergeEntity(), mergeData );
		// If managedEntity already corresponds with a different merge entity, that means
		// that there are multiple entities being merged that correspond with managedEntity.
		// In the following, oldMergeDataByEntityCopy will be replaced with mergeEntity in managedToMergeEntityXref.
		MergeData oldMergeDataByEntityCopy = entityCopyToMergeDataXref.put( mergeData.getEntityCopy(), mergeData );

		if ( oldMergeDataByMergeEntity == null ) {
			// this is a new mapping for mergeEntity in mergeToManagedEntityXref
			if  ( oldMergeDataByEntityCopy != null ) {
				// oldMergeDataByEntityCopy was a different merge entity with the same corresponding managed entity;
				entityCopyObserver.entityCopyDetected(
						mergeData.getEntityCopy(),
						mergeData.getMergeEntity(),
						oldMergeDataByEntityCopy.getMergeEntity(),
						getSession()
				);
			}
		}
		else {
			// mergeEntity was already mapped in mergeToManagedEntityXref
			if ( oldMergeDataByMergeEntity.getEntityCopy() != mergeData.getEntityCopy() ) {
				throw new IllegalArgumentException(
						"Error occurred while storing a merge Entity " + printEntity( mergeData.getMergeEntity() )
								+ ". It was previously associated with managed entity " + printEntity( oldMergeDataByMergeEntity.getEntityCopy() )
								+ ". Attempted to replace managed entity with " + printEntity( mergeData.getEntityCopy() )
				);
			}
		}

		return oldMergeDataByMergeEntity == null;
	}

	@Override
	public Collection<MergeData> getMergeEntityData() {
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
