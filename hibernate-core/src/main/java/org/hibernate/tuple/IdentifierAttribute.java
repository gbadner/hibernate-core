/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public interface IdentifierAttribute extends Attribute, Property {
	boolean isVirtual();

	boolean isEmbedded();

	IdentifierValue getUnsavedValue();

	IdentifierGenerator getIdentifierGenerator();

	boolean isIdentifierAssignedByInsert();

	boolean hasIdentifierMapper();

	default boolean hasEntityAssociation() {
		return containsEntityAssociation( getType() );
	}

	static boolean containsEntityAssociation(Type type) {
		if ( type.isComponentType() ) {
			return containsEntityAssociation( (CompositeType) type );
		}
		return false;
	}

	static boolean containsEntityAssociation(CompositeType compositeType) {
		for ( Type subtype : compositeType.getSubtypes() ) {
			if ( subtype.isAssociationType() ||
					( subtype.isComponentType() && containsEntityAssociation( (CompositeType) subtype ) ) ) {
				return true;
			}
		}
		return false;
	}
}
