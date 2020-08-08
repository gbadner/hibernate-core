/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import org.hibernate.EntityMode;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Second level cache providers now have the option to use custom key implementations.
 * This was done as the default key implementation is very generic and is quite
 * a large object to allocate in large quantities at runtime.
 * In some extreme cases, for example when the hit ratio is very low, this was making the efficiency
 * penalty vs its benefits tradeoff questionable.
 * <p/>
 * Depending on configuration settings there might be opportunities to
 * use simpler key implementations, for example when multi-tenancy is not being used to
 * avoid the tenant identifier, or when a cache instance is entirely dedicated to a single type
 * to use the primary id only, skipping the role or entity name.
 * <p/>
 * Even with multiple types sharing the same cache, their identifiers could be of the same
 * {@link org.hibernate.type.Type}; in this case the cache container could
 * use a single type reference to implement a custom equality function without having
 * to look it up on each equality check: that's a small optimisation but the
 * equality function is often invoked extremely frequently.
 * <p/>
 * Another reason is to make it more convenient to implement custom serialization protocols when the
 * implementation supports clustering.
 *
 * @see org.hibernate.type.Type#getHashCode(Object, SessionFactoryImplementor)
 * @see org.hibernate.type.Type#isEqual(Object, Object)
 * @author Sanne Grinovero
 * @since 5.0
 */
public class DefaultCacheKeysFactory implements CacheKeysFactory {
	public static final String SHORT_NAME = "default";
	public static final DefaultCacheKeysFactory INSTANCE = new DefaultCacheKeysFactory();

	public static Object staticCreateCollectionKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return new CacheKeyImplementation( id, persister.getKeyType(), persister.getRole(), tenantIdentifier, factory );
	}

	public static Object staticCreateEntityKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return new CacheKeyImplementation(
				maybeDisassembleId( id, persister, factory ),
				persister.getIdentifierType(),
				persister.getRootEntityName(),
				tenantIdentifier,
				factory
		);
	}

	private static Object maybeDisassembleId(Object id, EntityPersister persister, SessionFactoryImplementor factory) {
		final IdentifierProperty identifierProperty = persister.getEntityMetamodel().getIdentifierProperty();
		final Type identifierType = persister.getIdentifierType();
		if ( identifierType.isComponentType() && identifierProperty.hasEntityAssociation() ) {
			return disassembleCompositeValue( id, (CompositeType) identifierType, factory, persister.getEntityMode() );
		}
		else {
			return id;
		}
	}

	private static Object disassembleCompositeValue(
			Object compositeValue,
			CompositeType compositeType,
			SessionFactoryImplementor factory,
			EntityMode entityMode) {
		final Object[] values = compositeType.getPropertyValues( compositeValue, entityMode );
		final Type[] subtypes = compositeType.getSubtypes();
		for ( int i = 0; i < subtypes.length; i++ ) {
			if ( subtypes[i].isEntityType() ) {
				final EntityType associatedEntityType = (EntityType) subtypes[i];
				final EntityPersister associatedEntityPersister =
						factory.getMetamodel().entityPersister( associatedEntityType.getAssociatedEntityName() );
				values[i] = maybeDisassembleId(
						associatedEntityPersister.getEntityTuplizer().getIdentifier( values[i], null ),
						associatedEntityPersister,
						factory
				);
			}
			else if ( subtypes[i].isComponentType() ) {
				values[i] = disassembleCompositeValue( values[i], (CompositeType) subtypes[i], factory, entityMode );
			}
		}
		return values;
	}

	public static Object staticCreateNaturalIdKey(Object[] naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session) {
		return new NaturalIdCacheKey( naturalIdValues,  persister.getPropertyTypes(), persister.getNaturalIdentifierProperties(), persister.getRootEntityName(), session );
	}

	public static Object staticGetEntityId(Object cacheKey) {
		return ((CacheKeyImplementation) cacheKey).getId();
	}

	public static Object staticGetCollectionId(Object cacheKey) {
		return ((CacheKeyImplementation) cacheKey).getId();
	}

	public static Object[] staticGetNaturalIdValues(Object cacheKey) {
		return ((NaturalIdCacheKey) cacheKey).getNaturalIdValues();
	}

	@Override
	public Object createCollectionKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return staticCreateCollectionKey(id, persister, factory, tenantIdentifier);
	}

	@Override
	public Object createEntityKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return staticCreateEntityKey(id, persister, factory, tenantIdentifier);
	}

	@Override
	public Object createNaturalIdKey(Object[] naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session) {
		return staticCreateNaturalIdKey(naturalIdValues, persister, session);
	}

	@Override
	public Object getEntityId(Object cacheKey) {
		return staticGetEntityId(cacheKey);
	}

	@Override
	public Object getCollectionId(Object cacheKey) {
		return staticGetCollectionId(cacheKey);
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return staticGetNaturalIdValues(cacheKey);
	}
}
