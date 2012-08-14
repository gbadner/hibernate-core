/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

import static org.hibernate.id.EntityIdentifierNature.AGGREGATED_COMPOSITE;
import static org.hibernate.id.EntityIdentifierNature.COMPOSITE;
import static org.hibernate.id.EntityIdentifierNature.SIMPLE;

/**
 * Hold information about the entity identifier.  At a high-level, can be one of 2-types:<ul>
 *     <li>single-attribute identifier - this includes both simple identifiers and aggregated composite identifiers</li>
 *     <li>multiple-attribute identifier - non-aggregated composite identifiers</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class EntityIdentifier {
	private final EntityBinding entityBinding;
	private EntityIdentifierBinding entityIdentifierBinding;
	private IdentifierGenerator identifierGenerator;

	/**
	 * Create an identifier
	 *
	 * @param entityBinding the entity binding for which this instance is the id
	 */
	public EntityIdentifier(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public EntityBinding getEntityBinding() {
		return entityBinding;
	}

	public void prepareAsSimpleIdentifier(
			SingularNonAssociationAttributeBinding attributeBinding,
			IdGenerator idGenerator,
			String unsavedValue) {
		prepareAsSingleAttributeIdentifier( SIMPLE, attributeBinding, idGenerator, unsavedValue );
	}

	public void prepareAsAggregatedCompositeIdentifier(
			SingularNonAssociationAttributeBinding attributeBinding,
			IdGenerator idGenerator,
			String unsavedValue) {
		prepareAsSingleAttributeIdentifier( AGGREGATED_COMPOSITE, attributeBinding, idGenerator, unsavedValue );
	}

	protected void prepareAsSingleAttributeIdentifier(
			EntityIdentifierNature nature,
			SingularNonAssociationAttributeBinding attributeBinding,
			IdGenerator idGenerator,
			String unsavedValue) {
		ensureNotBound();
		this.entityIdentifierBinding =
				new SingleAttributeIdentifierBindingImpl( nature, attributeBinding, idGenerator, unsavedValue );
	}

	public void prepareAsNonAggregatedCompositeIdentifier(
			List<SingularNonAssociationAttributeBinding> attributeBindings,
			Class<?> idClassClass,
			MetaAttributeContext metaAttributeContext) {
		ensureNotBound();
		// TODO: where are IdGenerator and unsavedValue???
		this.entityIdentifierBinding =
				new CompositeIdentifierBindingImpl( null, null, attributeBindings, idClassClass, metaAttributeContext );
	}

	public EntityIdentifierNature getNature() {
		ensureBound();
		return entityIdentifierBinding.getNature();
	}

	public SingularNonAssociationAttributeBinding getAttributeBinding() {
		ensureBound();
		if ( isComposite() ) {
			throw new UnsupportedOperationException( "Unsupported for composite IDs" );
		}
		return ( (SingleAttributeIdentifierBindingImpl) entityIdentifierBinding ).getAttributeBinding();
	}

	public Iterable<SingularNonAssociationAttributeBinding> getAttributeBindings() {
		ensureBound();
		List<SingularNonAssociationAttributeBinding> attributeBindings  =
				isComposite() ?
						( (CompositeIdentifierBindingImpl) entityIdentifierBinding ).getAttributeBindings() :
						Collections.singletonList(
								( (SingleAttributeIdentifierBindingImpl) entityIdentifierBinding ).getAttributeBinding()
						);
		return Collections.unmodifiableList( attributeBindings );
	}

	public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
		ensureBound();
		return entityIdentifierBinding.isIdentifierAttributeBinding( attributeBinding );
	}

	public String getUnsavedValue() {
		ensureBound();
		return entityIdentifierBinding.getUnsavedValue();
	}

	public boolean isComposite() {
		ensureBound();
		return entityIdentifierBinding.getNature() == EntityIdentifierNature.COMPOSITE ;
	}

	public Class getIdClassClass() {
		ensureBound();
		return entityIdentifierBinding.getNature() != COMPOSITE ?
				null :
				( ( CompositeIdentifierBindingImpl ) entityIdentifierBinding ).getIdClassClass();
	}

	public boolean isIdentifierMapper() {
		return getIdClassClass() != null;
	}


	public MetaAttributeContext getMetaAttributeContext() {
		ensureBound();
		return entityIdentifierBinding.getMetaAttributeContext();
	}

	// todo do we really need this createIdentifierGenerator and how do we make sure the getter is not called too early
	// maybe some sort of visitor pattern here!? (HF)
	public IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory, Properties properties) {
		ensureBound();
		return entityIdentifierBinding.createIdentifierGenerator( factory, properties );
	}

	public IdentifierGenerator getIdentifierGenerator() {
		ensureBound();
		return identifierGenerator;
	}

	protected void ensureBound() {
		if ( ! isBound() ) {
			throw new IllegalStateException( "Entity identifier was not yet bound" );
		}
	}

	protected void ensureNotBound() {
		if ( isBound() ) {
			throw new IllegalStateException( "Entity identifier was already bound" );
		}
	}

	protected boolean isBound() {
		return entityIdentifierBinding != null;
	}

	public int getColumnCount() {
		ensureBound();
		return entityIdentifierBinding.getColumnCount();
	}

	private abstract class EntityIdentifierBinding {
		private final EntityIdentifierNature nature;
		private final IdGenerator idGenerator;
		private final String unsavedValue;
		private final MetaAttributeContext metaAttributeContext;


		protected EntityIdentifierBinding(
				EntityIdentifierNature nature,
				IdGenerator idGenerator,
				String unsavedValue,
				MetaAttributeContext metaAttributeContext) {
			this.nature = nature;
			this.idGenerator = idGenerator;
			this.unsavedValue = unsavedValue;
			this.metaAttributeContext = metaAttributeContext;
		}

		public EntityIdentifierNature getNature() {
			return nature;
		}

		public String getUnsavedValue() {
			return unsavedValue;
		}

		protected IdGenerator getIdGenerator() {
			return idGenerator;
		}

		public MetaAttributeContext getMetaAttributeContext() {
			return metaAttributeContext;
		}

		public abstract IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory, Properties properties);
		public abstract boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding);
		public abstract int getColumnCount();
	}

	private class SingleAttributeIdentifierBindingImpl extends EntityIdentifierBinding {
		private final SingularNonAssociationAttributeBinding identifierAttributeBinding;
		private final int columnCount;

		SingleAttributeIdentifierBindingImpl(
				EntityIdentifierNature nature,
				SingularNonAssociationAttributeBinding identifierAttributeBinding,
				IdGenerator idGenerator,
				String unsavedValue) {
			super( nature, idGenerator, unsavedValue, identifierAttributeBinding.getMetaAttributeContext() );
			this.identifierAttributeBinding = identifierAttributeBinding;
			this.columnCount = identifierAttributeBinding.getRelationalValueBindings().size();

			// Configure primary key in relational model
			for ( final RelationalValueBinding valueBinding : identifierAttributeBinding.getRelationalValueBindings() ) {
				entityBinding.getPrimaryTable().getPrimaryKey().addColumn( (Column) valueBinding.getValue() );
			}
		}

		public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
				return identifierAttributeBinding.equals( attributeBinding );
		}

		public int getColumnCount() {
			return columnCount;
		}

		public IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory, Properties properties) {
			if ( getIdGenerator() != null ) {
				identifierGenerator = identifierAttributeBinding.createIdentifierGenerator(
						getIdGenerator(),
						factory,
						properties
				);
			}
			return null;
		}

		public SingularNonAssociationAttributeBinding getAttributeBinding() {
			return identifierAttributeBinding;
		}
	}

	private class CompositeIdentifierBindingImpl extends EntityIdentifierBinding {
		private final Class idClassClass; // the class named in @IdClass
		private final int columnCount;
		private final List<SingularNonAssociationAttributeBinding> identifierAttributeBindings;

		CompositeIdentifierBindingImpl(
				IdGenerator idGenerator,
				String unsavedValue,
				List<SingularNonAssociationAttributeBinding> identifierAttributeBindings,
				Class<?> classReference,
				MetaAttributeContext metaAttributeContext) {
			super( COMPOSITE, idGenerator, unsavedValue, metaAttributeContext );
			this.identifierAttributeBindings =
					new ArrayList<SingularNonAssociationAttributeBinding>( identifierAttributeBindings );
			this.idClassClass = classReference;

			if ( identifierAttributeBindings.size() == 0 ) {
				throw new MappingException(
						"A composite ID has 0 attributes for " + entityBinding.getEntity().getName()
				);
			}
			identifierAttributeBindings = new ArrayList<SingularNonAssociationAttributeBinding>(
					identifierAttributeBindings.size()
			);
			int nColumns = 0;
			for ( SingularNonAssociationAttributeBinding attributeBinding : identifierAttributeBindings ) {
				nColumns += attributeBinding.getRelationalValueBindings().size();

				// Configure primary key in relational model
				for ( final RelationalValueBinding valueBinding : attributeBinding.getRelationalValueBindings() ) {
					entityBinding.getPrimaryTable().getPrimaryKey().addColumn( (Column) valueBinding.getValue() );
				}
			}
			this.columnCount = nColumns;
		}

		public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
			return attributeBinding instanceof SingularNonAssociationAttributeBinding &&
					identifierAttributeBindings.contains( attributeBinding );
		}

		public int getColumnCount() {
			return columnCount;
		}

		public Class getIdClassClass() {
			return idClassClass;
		}

		public IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory, Properties properties) {
			// TODO: implement this
			throw new NotYetImplementedException( "Creation of IdentifierGenerator is not implemented for composite IDs yet." );
		}

		public List<SingularNonAssociationAttributeBinding> getAttributeBindings() {
			return identifierAttributeBindings;
		}
	}
}
