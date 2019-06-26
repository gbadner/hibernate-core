/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.Collections;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.CollectionType;

/**
 * Process collections reachable from an entity. This
 * visitor assumes that wrap was already performed for
 * the entity.
 *
 * @author Gavin King
 */
public class FlushVisitor extends AbstractVisitor {
	private Object owner;

	FlushVisitor(EventSource session, Object owner) {
		super(session);
		this.owner = owner;
	}

	Object processCollection(Object collection, CollectionType type) throws HibernateException {
		
		if ( collection == CollectionType.UNFETCHED_COLLECTION ) {
			return null;
		}

		if ( collection != null ) {
			final PersistentCollection coll;
			if ( type.hasHolder() ) {
				coll = getSession().getPersistenceContext().getCollectionHolder(collection);
			}
			else if ( collection == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				coll = (PersistentCollection) type.resolve( collection, getSession(), owner );
			}
			else if ( PersistentCollection.class.isInstance( collection ) ){
				coll = (PersistentCollection) collection;
			}
			else {
				// collection is unwrapped.
				// There should already be a wrapped collection in the PersistenceContext, so look it up.
				final CollectionPersister collectionPersister =
						getSession().getFactory().getMetamodel().collectionPersister( type.getRole() );
				final PersistenceContext persistenceContext = getSession().getPersistenceContext();
				final CollectionKey collectionKey = new CollectionKey(
						collectionPersister,
						type.getKeyOfOwner( owner, getSession() )
				);
				coll = persistenceContext.getCollection( collectionKey );
				if ( coll == null ) {
					// This should never happen
					throw new AssertionFailure(
							"An unwrapped collection is being flushed that does not have a corresponding PersistentCollection in the PersistenceContext: " +
									MessageHelper.collectionInfoString( type.getRole(), collectionKey.getKey() )
					);
				}
			}

			Collections.processReachableCollection( coll, type, owner, getSession() );
		}

		return null;

	}

	@Override
	boolean includeEntityProperty(Object[] values, int i) {
		return true;
	}

}
