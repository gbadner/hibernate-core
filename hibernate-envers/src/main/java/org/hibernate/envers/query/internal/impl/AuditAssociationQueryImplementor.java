/**
 * 
 */
package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.Triple;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.internal.CriteriaTools;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;
import org.hibernate.envers.tools.Pair;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class AuditAssociationQueryImplementor<Q extends AuditQueryImplementor> implements AuditAssociationQuery<Q>, AuditQueryImplementor {

	private final EnversService enversService;;
	private final AuditReaderImplementor auditReader;
	private final Q parent;
	private final QueryBuilder queryBuilder;
	private final String entityName;
	private final IdMapper ownerAssociationIdMapper;
	private final String ownerAlias;
	private final String alias;
	private final List<AuditCriterion> criterions = new ArrayList<AuditCriterion>();
	private final Parameters parameters;
	private final List<AuditAssociationQueryImplementor<?>> associationQueries = new ArrayList<AuditAssociationQueryImplementor<?>>();
	private final Map<String, AuditAssociationQueryImplementor<AuditAssociationQueryImplementor<Q>>> associationQueryMap = new HashMap<String, AuditAssociationQueryImplementor<AuditAssociationQueryImplementor<Q>>>();
	private boolean hasProjections;
	private boolean hasOrders;

	public AuditAssociationQueryImplementor(final EnversService enversService, final AuditReaderImplementor auditReader, final Q parent,
			final QueryBuilder queryBuilder, final String ownerEntityName, final String propertyName, final String ownerAlias) {
		this.enversService = enversService;
		this.auditReader = auditReader;
		this.parent = parent;
		this.queryBuilder = queryBuilder;

		final RelationDescription relationDescription = CriteriaTools.getRelatedEntity( enversService, ownerEntityName, propertyName );
		if ( relationDescription == null ) {
			throw new IllegalArgumentException( "Property " + propertyName + " of entity " + ownerEntityName + " is not a valid association for queries" );
		}
		this.entityName = relationDescription.getToEntityName();
		this.ownerAssociationIdMapper = relationDescription.getIdMapper();
		this.ownerAlias = ownerAlias;
		alias = queryBuilder.generateAlias();
		parameters = queryBuilder.addParameters( alias );
	}

	@Override
	public List getResultList() throws AuditException {
		return parent.getResultList();
	}

	@Override
	public Object getSingleResult() throws AuditException, NonUniqueResultException, NoResultException {
		return parent.getSingleResult();
	}

	@Override
	public AuditAssociationQueryImplementor<AuditAssociationQueryImplementor<Q>> createCriteria(String associationName) {
		AuditAssociationQueryImplementor<AuditAssociationQueryImplementor<Q>> result = associationQueryMap.get( associationName );
		if (result == null) {
				result = new AuditAssociationQueryImplementor<AuditAssociationQueryImplementor<Q>>(
							enversService, auditReader, this, queryBuilder, entityName, associationName, alias );
				associationQueries.add( result );
				associationQueryMap.put( associationName, result );
		}
		return result;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> add(AuditCriterion criterion) {
		criterions.add( criterion );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> addProjection(AuditProjection projection) {
		hasProjections = true;
		Triple<String, String, Boolean> projectionData = projection.getData( enversService );
		String propertyName = CriteriaTools.determinePropertyName( enversService, auditReader, entityName, projectionData.getSecond() );
		queryBuilder.addProjection( projectionData.getFirst(), alias, propertyName, projectionData.getThird() );
		registerProjection( entityName, projection );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> addOrder(AuditOrder order) {
		hasOrders = true;
		Pair<String, Boolean> orderData = order.getData( enversService );
		String propertyName = CriteriaTools.determinePropertyName( enversService, auditReader, entityName, orderData.getFirst() );
		queryBuilder.addOrder( alias, propertyName, orderData.getSecond() );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> setMaxResults(int maxResults) {
		parent.setMaxResults( maxResults );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> setFirstResult(int firstResult) {
		parent.setFirstResult( firstResult );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> setCacheable(boolean cacheable) {
		parent.setCacheable( cacheable );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> setCacheRegion(String cacheRegion) {
		parent.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> setComment(String comment) {
		parent.setComment( comment );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> setFlushMode(FlushMode flushMode) {
		parent.setFlushMode( flushMode );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> setCacheMode(CacheMode cacheMode) {
		parent.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> setTimeout(int timeout) {
		parent.setTimeout( timeout );
		return this;
	}

	@Override
	public AuditAssociationQueryImplementor<Q> setLockMode(LockMode lockMode) {
		parent.setLockMode( lockMode );
		return this;
	}

	public Q up() {
		return parent;
	}

	protected boolean hasCriterions() {
		boolean result = !criterions.isEmpty();
		if ( !result ) {
			for ( final AuditAssociationQueryImplementor<?> sub : associationQueries ) {
				if ( sub.hasCriterions() ) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	protected boolean hasOrders() {
		boolean result = hasOrders;
		if ( !result ) {
			for ( final AuditAssociationQueryImplementor<?> sub : associationQueries ) {
				if ( sub.hasOrders() ) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	protected boolean hasProjections() {
		boolean result = hasProjections;
		if ( !result ) {
			for ( final AuditAssociationQueryImplementor<?> sub : associationQueries ) {
				if ( sub.hasProjections() ) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	protected void addCriterionsToQuery(AuditReaderImplementor versionsReader) {
		if ( hasCriterions() || hasOrders() || hasProjections() ) {
			if ( enversService.getEntitiesConfigurations().isVersioned( entityName ) ) {
				String auditEntityName = enversService.getAuditEntitiesConfiguration().getAuditEntityName( entityName );
				queryBuilder.addFrom( auditEntityName, alias, false );

				// owner.reference_id = target.originalId.id
				AuditEntitiesConfiguration verEntCfg = enversService.getAuditEntitiesConfiguration();
				String originalIdPropertyName = verEntCfg.getOriginalIdPropName();
				IdMapper idMapperTarget = enversService.getEntitiesConfigurations().get( entityName ).getIdMapper();
				final String prefix = alias.concat( "." ).concat( originalIdPropertyName );
				ownerAssociationIdMapper.addIdsEqualToQuery( queryBuilder.getRootParameters(), ownerAlias, idMapperTarget, prefix );

				// filter reference of target entity
				String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
				MiddleIdData referencedIdData = new MiddleIdData( verEntCfg, enversService.getEntitiesConfigurations().get( entityName ).getIdMappingData(), null, entityName,
						enversService.getEntitiesConfigurations().isVersioned( entityName ) );
				enversService.getAuditStrategy().addEntityAtRevisionRestriction( enversService.getGlobalConfiguration(), queryBuilder, parameters, revisionPropertyPath,
						verEntCfg.getRevisionEndFieldName(), true, referencedIdData, revisionPropertyPath, originalIdPropertyName, alias,
						queryBuilder.generateAlias(), true );
			}
			else {
				queryBuilder.addFrom( entityName, alias, false );
				// owner.reference_id = target.id
				IdMapper idMapperTarget = enversService.getEntitiesConfigurations().getNotVersionEntityConfiguration( entityName ).getIdMapper();
				ownerAssociationIdMapper.addIdsEqualToQuery( queryBuilder.getRootParameters(), ownerAlias, idMapperTarget, alias );
			}

			for ( AuditCriterion criterion : criterions ) {
				criterion.addToQuery( enversService, versionsReader, entityName, alias, queryBuilder, parameters );
			}

			for ( final AuditAssociationQueryImplementor<?> sub : associationQueries ) {
				sub.addCriterionsToQuery( versionsReader );
			}
		}

	}

	@Override
	public void registerProjection(final String entityName, AuditProjection projection) {
		parent.registerProjection( entityName, projection );
	}

}
