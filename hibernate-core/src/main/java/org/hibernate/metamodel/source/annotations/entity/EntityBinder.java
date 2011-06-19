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
package org.hibernate.metamodel.source.annotations.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.GenerationType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.binding.state.DiscriminatorBindingState;
import org.hibernate.metamodel.binding.state.ManyToOneAttributeBindingState;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.UniqueKey;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.entity.state.binding.AttributeBindingStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.binding.DiscriminatorBindingStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.binding.EntityBindingStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.binding.ManyToOneBindingStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.relational.ColumnRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.relational.ManyToOneRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.relational.TupleRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.global.IdGeneratorBinder;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.MetadataImplementor;

/**
 * Creates the domain and relational metamodel for a configured class and <i>binds</i> them together.
 *
 * @author Hardy Ferentschik
 */
public class EntityBinder {
	private final ConfiguredClass configuredClass;
	private final MetadataImplementor meta;

	private Schema.Name schemaName;

	public EntityBinder(MetadataImplementor metadata, ConfiguredClass configuredClass) {
		this.configuredClass = configuredClass;
		this.meta = metadata;
	}

	public void bind() {
		EntityBinding entityBinding = new EntityBinding();
		EntityBindingStateImpl entityBindingState = new EntityBindingStateImpl( configuredClass );

		bindJpaEntityAnnotation( entityBinding, entityBindingState );
		bindHibernateEntityAnnotation( entityBindingState ); // optional hibernate specific @org.hibernate.annotations.Entity

		schemaName = createSchemaName();
		bindTable( entityBinding );

		bindInheritance( entityBinding );

		// bind entity level annotations
		bindWhereFilter( entityBindingState );
		bindJpaCaching( entityBindingState );
		bindHibernateCaching( entityBindingState );
		bindProxy( entityBindingState );

		// TODO: cannot currently bind POJO representation until proxy is bound; should be fixed
		bindPojoRepresentation( entityBinding );

		bindSynchronize( entityBindingState );
		bindCustomSQL( entityBindingState );
		bindRowId( entityBindingState );
		bindBatchSize( entityBindingState );

		// take care of the id, attributes and relations
		if ( configuredClass.isRoot() ) {
			bindId( entityBinding );
		}

		// bind all attributes - simple as well as associations
		bindAttributes( entityBinding );
		bindTableUniqueConstraints( entityBinding );

		// last, but not least we initialize and register the new EntityBinding
		entityBinding.initialize( entityBindingState );
		meta.addEntity( entityBinding );
	}

	private void bindTableUniqueConstraints(EntityBinding entityBinding) {
		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(),
				JPADotNames.TABLE
		);
		if ( tableAnnotation == null ) {
			return;
		}
		TableSpecification table = entityBinding.getBaseTable();
		bindUniqueConstraints( tableAnnotation, table );
	}

	/**
	 * Bind {@link javax.persistence.UniqueConstraint} to table as a {@link UniqueKey}
	 *
	 * @param tableAnnotation JPA annotations which has a {@code uniqueConstraints} attribute.
	 * @param table Table which the UniqueKey bind to.
	 */
	private void bindUniqueConstraints(AnnotationInstance tableAnnotation, TableSpecification table) {
		AnnotationValue value = tableAnnotation.value( "uniqueConstraints" );
		if ( value == null ) {
			return;
		}
		AnnotationInstance[] uniqueConstraints = value.asNestedArray();
		for ( AnnotationInstance unique : uniqueConstraints ) {
			String name = unique.value( "name" ).asString();
			UniqueKey uniqueKey = table.getOrCreateUniqueKey( name );
			String[] columnNames = unique.value( "columnNames" ).asStringArray();
			if ( columnNames.length == 0 ) {
				//todo throw exception?
			}
			for ( String columnName : columnNames ) {
				uniqueKey.addColumn( table.getOrCreateColumn( columnName ) );
			}
		}
	}

	private void bindInheritance(EntityBinding entityBinding) {
		entityBinding.setInheritanceType( configuredClass.getInheritanceType() );
		switch ( configuredClass.getInheritanceType() ) {
			case SINGLE_TABLE: {
				bindDiscriminatorColumn( entityBinding );
				break;
			}
			case JOINED: {
				// todo
				break;
			}
			case TABLE_PER_CLASS: {
				// todo
				break;
			}
			default: {
				// do nothing
			}
		}
	}

	private void bindDiscriminatorColumn(EntityBinding entityBinding) {
		final Map<DotName, List<AnnotationInstance>> typeAnnotations = JandexHelper.getTypeAnnotations(
				configuredClass.getClassInfo()
		);
		SimpleAttribute discriminatorAttribute = SimpleAttribute.createDiscriminatorAttribute( typeAnnotations );

		bindSingleMappedAttribute( entityBinding, discriminatorAttribute );

		if ( !( discriminatorAttribute.getColumnValues() instanceof DiscriminatorColumnValues ) ) {
			throw new AssertionFailure( "Expected discriminator column values" );
		}
	}

	private void bindWhereFilter(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.WHERE
		);
		if ( whereAnnotation != null ) {
			// no null check needed, it is a required attribute
			String clause = whereAnnotation.value( "clause" ).asString();
			entityBindingState.setWhereFilter( clause );
		}
	}

	private void bindHibernateCaching(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance cacheAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.CACHE
		);
		if ( cacheAnnotation == null ) {
			return;
		}

		String region;
		if ( cacheAnnotation.value( "region" ) != null ) {
			region = cacheAnnotation.value( "region" ).asString();
		}
		else {
			region = entityBindingState.getEntityName();
		}

		boolean cacheLazyProperties = true;
		if ( cacheAnnotation.value( "include" ) != null ) {
			String tmp = cacheAnnotation.value( "include" ).asString();
			if ( "all".equalsIgnoreCase( tmp ) ) {
				cacheLazyProperties = true;
			}
			else if ( "non-lazy".equalsIgnoreCase( tmp ) ) {
				cacheLazyProperties = false;
			}
			else {
				throw new AnnotationException( "Unknown lazy property annotations: " + tmp );
			}
		}

		CacheConcurrencyStrategy strategy = CacheConcurrencyStrategy.valueOf(
				cacheAnnotation.value( "usage" ).asEnum()
		);
		Caching caching = new Caching( region, strategy.toAccessType(), cacheLazyProperties );
		entityBindingState.setCaching( caching );
	}

	// This does not take care of any inheritance of @Cacheable within a class hierarchy as specified in JPA2.
	// This is currently not supported (HF)
	private void bindJpaCaching(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance cacheAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.CACHEABLE
		);

		boolean cacheable = true; // true is the default
		if ( cacheAnnotation != null && cacheAnnotation.value() != null ) {
			cacheable = cacheAnnotation.value().asBoolean();
		}

		Caching caching = null;
		switch ( meta.getOptions().getSharedCacheMode() ) {
			case ALL: {
				caching = createCachingForCacheableAnnotation( entityBindingState );
				break;
			}
			case ENABLE_SELECTIVE: {
				if ( cacheable ) {
					caching = createCachingForCacheableAnnotation( entityBindingState );
				}
				break;
			}
			case DISABLE_SELECTIVE: {
				if ( cacheAnnotation == null || cacheable ) {
					caching = createCachingForCacheableAnnotation( entityBindingState );
				}
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				break;
			}
		}
		if ( caching != null ) {
			entityBindingState.setCaching( caching );
		}
	}

	private void bindProxy(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance proxyAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.PROXY
		);
		boolean lazy = true;
		String proxyInterfaceClass = null;

		if ( proxyAnnotation != null ) {
			AnnotationValue lazyValue = proxyAnnotation.value( "lazy" );
			if ( lazyValue != null ) {
				lazy = lazyValue.asBoolean();
			}

			AnnotationValue proxyClassValue = proxyAnnotation.value( "proxyClass" );
			if ( proxyClassValue != null ) {
				proxyInterfaceClass = proxyClassValue.asString();
			}
		}

		entityBindingState.setLazy( lazy );
		entityBindingState.setProxyInterfaceName( proxyInterfaceClass );
	}

	private void bindPojoRepresentation(EntityBinding entityBinding) {
		Class clazz = configuredClass.getClass();
		String proxyName = entityBinding.getProxyInterfaceName();

		entityBinding.getEntity().getPojoEntitySpecifics().setClassHolder(
			meta.getLoadedClassHolder( clazz )
		);

		if ( proxyName != null ) {
			entityBinding.getEntity().getPojoEntitySpecifics().setProxyInterfaceName( proxyName );
			entityBinding.setLazy( true );
		}
		else if ( entityBinding.isLazy() ) {
			entityBinding.getEntity().getPojoEntitySpecifics().setProxyInterfaceName( clazz.getName() );
		}

		// TODO: locate tuplizer
		//if ( tuplizer != null ) {
		//	entityBinding.getEntity().getPojoEntitySpecifics().setTuplizerClassName( tuplizer.getClazz() );
		//}
	}

	private void bindSynchronize(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance synchronizeAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SYNCHRONIZE
		);

		if ( synchronizeAnnotation != null ) {
			String[] tableNames = synchronizeAnnotation.value().asStringArray();
			for ( String tableName : tableNames ) {
				entityBindingState.addSynchronizedTableName( tableName );
			}
		}
	}

	private void bindCustomSQL(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance sqlInsertAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SQL_INSERT
		);
		entityBindingState.setCustomInsert( createCustomSQL( sqlInsertAnnotation ) );

		AnnotationInstance sqlUpdateAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SQL_UPDATE
		);
		entityBindingState.setCustomUpdate( createCustomSQL( sqlUpdateAnnotation ) );

		AnnotationInstance sqlDeleteAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SQL_DELETE
		);
		entityBindingState.setCustomDelete( createCustomSQL( sqlDeleteAnnotation ) );

		AnnotationInstance sqlDeleteAllAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SQL_DELETE_ALL
		);
		if ( sqlDeleteAllAnnotation != null ) {
			entityBindingState.setCustomDelete( createCustomSQL( sqlDeleteAllAnnotation ) );
		}
	}

	private CustomSQL createCustomSQL(AnnotationInstance customSQLAnnotation) {
		if ( customSQLAnnotation == null ) {
			return null;
		}

		String sql = customSQLAnnotation.value( "sql" ).asString();
		boolean isCallable = false;
		AnnotationValue callableValue = customSQLAnnotation.value( "callable" );
		if ( callableValue != null ) {
			isCallable = callableValue.asBoolean();
		}

		ResultCheckStyle checkStyle = ResultCheckStyle.NONE;
		AnnotationValue checkStyleValue = customSQLAnnotation.value( "check" );
		if ( checkStyleValue != null ) {
			checkStyle = Enum.valueOf( ResultCheckStyle.class, checkStyleValue.asEnum() );
		}

		return new CustomSQL(
				sql,
				isCallable,
				Enum.valueOf( ExecuteUpdateResultCheckStyle.class, checkStyle.toString() )
		);
	}

	private void bindRowId(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance rowIdAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.ROW_ID
		);

		if ( rowIdAnnotation != null ) {
			entityBindingState.setRowId( rowIdAnnotation.value().asString() );
		}
	}

	private void bindBatchSize(EntityBindingStateImpl entityBindingState) {
		AnnotationInstance batchSizeAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.BATCH_SIZE
		);

		if ( batchSizeAnnotation != null ) {
			entityBindingState.setBatchSize( batchSizeAnnotation.value( "size" ).asInt() );
		}
	}

	private Caching createCachingForCacheableAnnotation(EntityBindingStateImpl entityBindingState) {
		String region = entityBindingState.getEntityName();
		RegionFactory regionFactory = meta.getServiceRegistry().getService( RegionFactory.class );
		AccessType defaultAccessType = regionFactory.getDefaultAccessType();
		return new Caching( region, defaultAccessType, true );
	}

	private Schema.Name createSchemaName() {
		String schema = null;
		String catalog = null;

		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.TABLE
		);
		if ( tableAnnotation != null ) {
			AnnotationValue schemaValue = tableAnnotation.value( "schema" );
			AnnotationValue catalogValue = tableAnnotation.value( "catalog" );

			schema = schemaValue != null ? schemaValue.asString() : null;
			catalog = catalogValue != null ? catalogValue.asString() : null;
		}

		return new Schema.Name( schema, catalog );
	}

	private void bindTable(EntityBinding entityBinding) {
		final Schema schema = meta.getDatabase().getSchema( schemaName );
		final Identifier tableName = Identifier.toIdentifier( configuredClass.getPrimaryTableName() );
		org.hibernate.metamodel.relational.Table table = schema.getTable( tableName );
		if ( table == null ) {
			table = schema.createTable( tableName );
		}
		entityBinding.setBaseTable( table );

		AnnotationInstance checkAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.CHECK
		);
		if ( checkAnnotation != null ) {
			table.addCheckConstraint( checkAnnotation.value( "constraints" ).asString() );
		}
	}

	private void bindId(EntityBinding entityBinding) {
		switch ( configuredClass.getIdType() ) {
			case SIMPLE: {
				bindSingleIdAnnotation( entityBinding );
				break;
			}
			case COMPOSED: {
				// todo
				break;
			}
			case EMBEDDED: {
				// todo
				break;
			}
			default: {
			}
		}
	}


	private void bindJpaEntityAnnotation(EntityBinding entityBinding, EntityBindingStateImpl entityBindingState) {
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.ENTITY
		);
		String name;
		if ( jpaEntityAnnotation.value( "name" ) == null ) {
			name = configuredClass.getName();
		}
		else {
			name = jpaEntityAnnotation.value( "name" ).asString();
		}
		entityBindingState.setEntityName( name );
		entityBinding.setEntity( new Entity( name, getSuperType() ) );
	}

	private void bindSingleIdAnnotation(EntityBinding entityBinding) {
		AnnotationInstance idAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.ID
		);

		String idName = JandexHelper.getPropertyName( idAnnotation.target() );
		MappedAttribute idAttribute = configuredClass.getMappedProperty( idName );
		if ( !( idAttribute instanceof SimpleAttribute ) ) {
			throw new AssertionFailure( "Unexpected attribute type for id attribute" );
		}

		entityBinding.getEntity().getOrCreateSingularAttribute( idName );

		SimpleAttributeBinding attributeBinding = entityBinding.makeSimpleIdAttributeBinding( idName );
		attributeBinding.initialize( new AttributeBindingStateImpl( (SimpleAttribute) idAttribute ) );
		attributeBinding.initialize( new ColumnRelationalStateImpl( (SimpleAttribute) idAttribute, meta ) );
		bindSingleIdGeneratedValue( entityBinding, idName );

	}

	private void bindSingleIdGeneratedValue(EntityBinding entityBinding, String idPropertyName) {
		AnnotationInstance generatedValueAnn = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.GENERATED_VALUE
		);
		if ( generatedValueAnn == null ) {
			return;
		}

		String idName = JandexHelper.getPropertyName( generatedValueAnn.target() );
		if ( !idPropertyName.equals( idName ) ) {
			throw new AssertionFailure(
					String.format(
							"Attribute[%s.%s] with @GeneratedValue doesn't have a @Id.",
							configuredClass.getName(),
							idPropertyName
					)
			);
		}
		String generator = JandexHelper.getValueAsString( generatedValueAnn, "generator" );
		IdGenerator idGenerator = null;
		if ( StringHelper.isNotEmpty( generator ) ) {
			idGenerator = meta.getIdGenerator( generator );
			if ( idGenerator == null ) {
				throw new MappingException(
						String.format(
								"@GeneratedValue on %s.%s refering an undefined generator [%s]",
								configuredClass.getName(),
								idName,
								generator
						)
				);
			}
			entityBinding.getEntityIdentifier().setIdGenerator( idGenerator );
		}
		GenerationType generationType = JandexHelper.getValueAsEnum(
				generatedValueAnn,
				"strategy",
				GenerationType.class
		);
		String strategy = IdGeneratorBinder.generatorType(
				generationType,
				meta.getOptions().useNewIdentifierGenerators()
		);
		if ( idGenerator != null && !strategy.equals( idGenerator.getStrategy() ) ) {
			//todo how to ?
			throw new MappingException(
					String.format(
							"Inconsistent Id Generation strategy of @GeneratedValue on %s.%s",
							configuredClass.getName(),
							idName
					)
			);
		}
		else {
			idGenerator = new IdGenerator( "NAME", strategy, new HashMap<String, String>() );
			entityBinding.getEntityIdentifier().setIdGenerator( idGenerator );
		}
	}


	private void bindAttributes(EntityBinding entityBinding) {
		for ( MappedAttribute mappedAttribute : configuredClass.getMappedAttributes() ) {
			if ( mappedAttribute instanceof AssociationAttribute ) {
				bindAssociationAttribute( entityBinding, (AssociationAttribute) mappedAttribute );
			}
			else {
				bindSingleMappedAttribute( entityBinding, (SimpleAttribute) mappedAttribute );
			}
		}
	}

	private void bindAssociationAttribute(EntityBinding entityBinding, AssociationAttribute associationAttribute) {
		switch ( associationAttribute.getAssociationType() ) {
			case MANY_TO_ONE: {
				entityBinding.getEntity().getOrCreateSingularAttribute( associationAttribute.getName() );
				ManyToOneAttributeBinding manyToOneAttributeBinding = entityBinding.makeManyToOneAttributeBinding(
						associationAttribute.getName()
				);

				ManyToOneAttributeBindingState bindingState = new ManyToOneBindingStateImpl( associationAttribute );
				manyToOneAttributeBinding.initialize( bindingState );

				ManyToOneRelationalStateImpl relationalState = new ManyToOneRelationalStateImpl();
				if ( configuredClass.hasOwnTable() ) {
					ColumnRelationalStateImpl columnRelationsState = new ColumnRelationalStateImpl(
							associationAttribute, meta
					);
					relationalState.addValueState( columnRelationsState );
				}
				manyToOneAttributeBinding.initialize( relationalState );
				break;
			}
			default: {
				// todo
			}
		}
	}

	private void bindSingleMappedAttribute(EntityBinding entityBinding, SimpleAttribute simpleAttribute) {
		if ( simpleAttribute.isId() ) {
			return;
		}

		String attributeName = simpleAttribute.getName();
		entityBinding.getEntity().getOrCreateSingularAttribute( attributeName );
		SimpleAttributeBinding attributeBinding;

		if ( simpleAttribute.isDiscriminator() ) {
			EntityDiscriminator entityDiscriminator = entityBinding.makeEntityDiscriminator( attributeName );
			DiscriminatorBindingState bindingState = new DiscriminatorBindingStateImpl( simpleAttribute );
			entityDiscriminator.initialize( bindingState );
			attributeBinding = entityDiscriminator.getValueBinding();
		}
		else if ( simpleAttribute.isVersioned() ) {
			attributeBinding = entityBinding.makeVersionBinding( attributeName );
			SimpleAttributeBindingState bindingState = new AttributeBindingStateImpl( simpleAttribute );
			attributeBinding.initialize( bindingState );
		}
		else {
			attributeBinding = entityBinding.makeSimpleAttributeBinding( attributeName );
			SimpleAttributeBindingState bindingState = new AttributeBindingStateImpl( simpleAttribute );
			attributeBinding.initialize( bindingState );
		}

		if ( configuredClass.hasOwnTable() ) {
			ColumnRelationalStateImpl columnRelationsState = new ColumnRelationalStateImpl(
					simpleAttribute, meta
			);
			TupleRelationalStateImpl relationalState = new TupleRelationalStateImpl();
			relationalState.addValueState( columnRelationsState );

			attributeBinding.initialize( relationalState );
		}
	}

	private void bindHibernateEntityAnnotation(EntityBindingStateImpl entityBindingState) {
		// initialize w/ the defaults
		boolean mutable = true;
		boolean dynamicInsert = false;
		boolean dynamicUpdate = false;
		boolean selectBeforeUpdate = false;
		PolymorphismType polymorphism = PolymorphismType.IMPLICIT;
		OptimisticLockType optimisticLock = OptimisticLockType.VERSION;

		AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.ENTITY
		);

		if ( hibernateEntityAnnotation != null ) {
			if ( hibernateEntityAnnotation.value( "mutable" ) != null ) {
				mutable = hibernateEntityAnnotation.value( "mutable" ).asBoolean();
			}

			if ( hibernateEntityAnnotation.value( "dynamicInsert" ) != null ) {
				dynamicInsert = hibernateEntityAnnotation.value( "dynamicInsert" ).asBoolean();
			}

			if ( hibernateEntityAnnotation.value( "dynamicUpdate" ) != null ) {
				dynamicUpdate = hibernateEntityAnnotation.value( "dynamicUpdate" ).asBoolean();
			}

			if ( hibernateEntityAnnotation.value( "selectBeforeUpdate" ) != null ) {
				selectBeforeUpdate = hibernateEntityAnnotation.value( "selectBeforeUpdate" ).asBoolean();
			}

			if ( hibernateEntityAnnotation.value( "polymorphism" ) != null ) {
				polymorphism = PolymorphismType.valueOf( hibernateEntityAnnotation.value( "polymorphism" ).asEnum() );
			}

			if ( hibernateEntityAnnotation.value( "optimisticLock" ) != null ) {
				optimisticLock = OptimisticLockType.valueOf(
						hibernateEntityAnnotation.value( "optimisticLock" ).asEnum()
				);
			}

			if ( hibernateEntityAnnotation.value( "persister" ) != null ) {
				String persister = ( hibernateEntityAnnotation.value( "persister" ).toString() );
				entityBindingState.setPersisterClassHolder( meta.getClassHolder( persister ) );
			}
		}

		// also check for the immutable annotation
		AnnotationInstance immutableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.IMMUTABLE
		);
		if ( immutableAnnotation != null ) {
			mutable = false;
		}

		entityBindingState.setMutable( mutable );
		entityBindingState.setDynamicInsert( dynamicInsert );
		entityBindingState.setDynamicUpdate( dynamicUpdate );
		entityBindingState.setSelectBeforeUpdate( selectBeforeUpdate );
		entityBindingState.setExplicitPolymorphism( PolymorphismType.EXPLICIT.equals( polymorphism ) );
		entityBindingState.setOptimisticLock( optimisticLock );
	}

	private Hierarchical getSuperType() {
		ConfiguredClass parent = configuredClass.getParent();
		if ( parent == null ) {
			return null;
		}

		EntityBinding parentBinding = meta.getEntityBinding( parent.getName() );
		if ( parentBinding == null ) {
			throw new AssertionFailure(
					"Parent entity " + parent.getName() + " of entity " + configuredClass.getName() + " not yet created!"
			);
		}

		return parentBinding.getEntity();
	}
}

