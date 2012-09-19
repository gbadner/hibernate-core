/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.Composite;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * @author Steve Ebersole
 */
public class CompositeAttributeBinding
		extends AbstractSingularAttributeBinding
		implements SingularNonAssociationAttributeBinding, AttributeBindingContainer {
	private final String path;
	private final SingularAttribute parentReference;
	private Map<String, AttributeBinding> attributeBindingMap;
	private Class<?> externalAggregatingClass;
	private String externalAggregatingPropertyAccessorName;

	public CompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference) {
		this(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				parentReference,
				null,
				null,
				null
		);
	}

	public CompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			List<SingularAttributeBinding> subAttributeBindings,
			Class<?> externalAggregatingClass,
			String externalAggregatingPropertyAccessorName) {
		this(
				container,
				attribute,
				propertyAccessorName,
				false,
				false,
				naturalIdMutability,
				metaAttributeContext,
				null,
				subAttributeBindings,
				externalAggregatingClass,
				externalAggregatingPropertyAccessorName
		);
	}

	private CompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference,
			List<SingularAttributeBinding> subAttributeBindings,
			Class<?> externalAggregatingClass,
			String externalAggregatingPropertyAccessorName) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext
		);

		this.parentReference = parentReference;
		this.path = container.getPathBase() + '.' + attribute.getName();

		if ( subAttributeBindings == null ) {
			attributeBindingMap = new LinkedHashMap<String, AttributeBinding>();
		}
		else {
			Map<String, AttributeBinding> map = new LinkedHashMap<String, AttributeBinding>();
			for ( SingularAttributeBinding attributeBinding : subAttributeBindings ) {
				map.put( attributeBinding.getAttribute().getName(), attributeBinding );
			}
			attributeBindingMap = Collections.unmodifiableMap( map );
		}

		this.externalAggregatingClass = externalAggregatingClass;
		this.externalAggregatingPropertyAccessorName = externalAggregatingPropertyAccessorName;
	}

	@Override
	public List<RelationalValueBinding> getRelationalValueBindings() {
		final List<RelationalValueBinding> bindings = new ArrayList<RelationalValueBinding>();
		collectRelationalValueBindings( bindings );
		return bindings;
	}

	@Override
	protected void collectRelationalValueBindings(List<RelationalValueBinding> valueBindings) {
		for ( AttributeBinding subAttributeBinding : attributeBindings() ) {
			if ( AbstractSingularAttributeBinding.class.isInstance( subAttributeBinding ) ) {
				( (AbstractSingularAttributeBinding) subAttributeBinding ).collectRelationalValueBindings( valueBindings );
			}
		}
	}

	@Override
	public EntityBinding seekEntityBinding() {
		return getContainer().seekEntityBinding();
	}

	@Override
	public String getPathBase() {
		return path;
	}

	@Override
	public AttributeContainer getAttributeContainer() {
		return (AttributeContainer) getAttribute().getSingularAttributeType();
	}

	public boolean isAggregated() {
		return ! getAttribute().isSynthetic();
	}

	public Class<?> getExternalAggregatingClass() {
		return externalAggregatingClass;
	}

	public String getExternalAggregatingPropertyAccessorName() {
		return externalAggregatingPropertyAccessorName;
	}

	public Composite getComposite() {
		if ( !isAggregated() ) {
			throw new UnsupportedOperationException(
					String.format(
							"Operation is unsupported for non-aggregated %s objects.",
							getClass().getSimpleName()
					)
			);
		}
		return (Composite) getAttribute().getSingularAttributeType();
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	public boolean hasDerivedValue() {
		// todo : not sure this is even relevant for components
		return false;
	}

	@Override
	public boolean isNullable() {
		// return false if there are any singular attributes are non-nullable
		for ( AttributeBinding attributeBinding : attributeBindings() ) {
			// only check singular attributes
			if ( attributeBinding.getAttribute().isSingular() &&
					! ( (SingularAttributeBinding) attributeBinding ).isNullable() ) {
				return false;
			}
		}
		return true;
	}


	@Override
	public boolean isIncludedInInsert() {
		// if the attribute is synthetic, this attribute binding (as a whole) is not insertable;
		if ( getAttribute().isSynthetic() ) {
			return false;
		}
		// otherwise, return true if there are any singular attributes that are included in the insert.
		for ( AttributeBinding attributeBinding : attributeBindings() ) {
			// only check singular attributes
			if ( attributeBinding.getAttribute().isSingular() &&
					( (SingularAttributeBinding) attributeBinding ).isIncludedInInsert() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isIncludedInUpdate() {
		// if the attribute is synthetic, this attribute binding (as a whole) is not updateable;
		if ( getAttribute().isSynthetic() ) {
			return false;
		}
		// otherwise, return true if there are any singular attributes that are updatable;
		for ( AttributeBinding attributeBinding : attributeBindings() ) {
			// only check singular attributes
			if ( attributeBinding.getAttribute().isSingular() &&
					( (SingularAttributeBinding) attributeBinding ).isIncludedInUpdate() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IdentifierGenerator createIdentifierGenerator(
			IdGenerator idGenerator, IdentifierGeneratorFactory factory, Properties properties) {
		// for now...
		return null;
	}

	@Override
	public AttributeBinding locateAttributeBinding(String name) {
		return attributeBindingMap.get( name );
	}

	@Override
	public AttributeBinding locateAttributeBinding(List<org.hibernate.metamodel.spi.relational.Value> values) {
		for ( final AttributeBinding attributeBinding : attributeBindingMap.values() ) {
			if ( !BasicAttributeBinding.class.isInstance( attributeBinding ) ) {
				continue;
			}
			final BasicAttributeBinding basicAttributeBinding = (BasicAttributeBinding) attributeBinding;
			if ( basicAttributeBinding.getRelationalValueBindings().equals( values ) ) {
				return attributeBinding;
			}
		}
		return null;
	}

	public int attributeBindingSpan() {
		return attributeBindingMap.size();
	}

	@Override
	public Iterable<AttributeBinding> attributeBindings() {
		return attributeBindingMap.values();
	}

	@Override
	public BasicAttributeBinding makeBasicAttributeBinding(
			SingularAttribute attribute,
			List<RelationalValueBinding> relationalValueBindings,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			PropertyGeneration generation) {
		final BasicAttributeBinding binding = new BasicAttributeBinding(
				this,
				attribute,
				relationalValueBindings,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				generation
		);
		registerAttributeBinding( binding );
		return binding;
	}

	protected void registerAttributeBinding(AttributeBinding attributeBinding) {
		// todo : hook this into the EntityBinding notion of "entity referencing attribute bindings"
		attributeBindingMap.put( attributeBinding.getAttribute().getName(), attributeBinding );
	}

	@Override
	public CompositeAttributeBinding makeCompositeAttributeBinding(
			SingularAttribute attribute,
			SingularAttribute parentReferenceAttribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext) {
		final CompositeAttributeBinding binding = new CompositeAttributeBinding(
				this,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				parentReferenceAttribute
		);
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public ManyToOneAttributeBinding makeManyToOneAttributeBinding(
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding,
			List<RelationalValueBinding> valueBindings) {
		final ManyToOneAttributeBinding binding = new ManyToOneAttributeBinding(
				this,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				referencedEntityBinding,
				referencedAttributeBinding,
				valueBindings
		);
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public BagBinding makeBagAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttribute.Nature.BAG );
		final BagBinding binding = new BagBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext
		);
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public ListBinding makeListAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			int base) {
		Helper.checkPluralAttributeNature( attribute, PluralAttribute.Nature.LIST );
		final ListBinding binding = new ListBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				base );
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public MapBinding makeMapAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature elementNature,
			PluralAttributeIndexBinding.Nature indexNature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttribute.Nature.MAP );
		final MapBinding binding = new MapBinding(
				this,
				attribute,
				elementNature,
				indexNature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext );
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public SetBinding makeSetAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttribute.Nature.SET );
		final SetBinding binding = new SetBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext
		);
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public Class<?> getClassReference() {
		return getAttributeContainer().getClassReference();
	}

	public SingularAttribute getParentReference() {
		return parentReference;
	}
}
