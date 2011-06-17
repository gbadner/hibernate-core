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
package org.hibernate.metamodel.source.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.classmate.ResolvedType;
import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.DuplicateMappingException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.id.factory.DefaultIdentifierGeneratorFactory;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.SourceProcessingOrder;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.relational.Database;
import org.hibernate.metamodel.source.annotations.AnnotationBinder;
import org.hibernate.metamodel.source.hbm.HbmBinder;
import org.hibernate.metamodel.source.spi.Binder;
import org.hibernate.metamodel.source.spi.ClassHolder;
import org.hibernate.metamodel.source.spi.MappingDefaults;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

/**
 * Container for configuration data collected during binding the metamodel.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class MetadataImpl implements MetadataImplementor, Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MetadataImpl.class.getName()
	);

	private final BasicServiceRegistry serviceRegistry;
	private final Options options;
	private ClassLoaderService classLoaderService;

	private TypeResolver typeResolver = new TypeResolver();

	private SessionFactoryBuilder sessionFactoryBuilder = new SessionFactoryBuilderImpl( this );

	private DefaultIdentifierGeneratorFactory identifierGeneratorFactory = new DefaultIdentifierGeneratorFactory();

	private final Database database = new Database();

	private final MappingDefaults mappingDefaults;

	/**
	 * Maps the fully qualified class name of an entity to its entity binding
	 */
	private Map<String, EntityBinding> entityBindingMap = new HashMap<String, EntityBinding>();
	private Map<String, EntityBinding> rootEntityBindingMap = new HashMap<String, EntityBinding>();
	private Map<String, PluralAttributeBinding> collectionBindingMap = new HashMap<String, PluralAttributeBinding>();
	private Map<String, FetchProfile> fetchProfiles = new HashMap<String, FetchProfile>();
	private Map<String, String> imports = new HashMap<String, String>();
	private Map<String, TypeDef> typeDefs = new HashMap<String, TypeDef>();
	private Map<String, IdGenerator> idGenerators = new HashMap<String, IdGenerator>();
	private Map<String, NamedQueryDefinition> namedQueryDefs = new HashMap<String, NamedQueryDefinition>();
	private Map<String, NamedSQLQueryDefinition> namedNativeQueryDefs = new HashMap<String, NamedSQLQueryDefinition>();
	private Map<String, ResultSetMappingDefinition> resultSetMappings = new HashMap<String, ResultSetMappingDefinition>();
	private Map<String, FilterDefinition> filterDefs = new HashMap<String, FilterDefinition>();

	private Map<String, ClassHolder> classHoldersByName = new HashMap<String, ClassHolder>();

	// todo : keep as part of Database?
	private List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects = new ArrayList<AuxiliaryDatabaseObject>();

	public MetadataImpl(MetadataSources metadataSources, Options options) {
		this.serviceRegistry = metadataSources.getServiceRegistry();
		this.options = options;

		this.mappingDefaults = new MappingDefaultsImpl();

		final Binder[] binders;
		if ( options.getSourceProcessingOrder() == SourceProcessingOrder.HBM_FIRST ) {
			binders = new Binder[] {
					new HbmBinder( this ),
					new AnnotationBinder( this )
			};
		}
		else {
			binders = new Binder[] {
					new AnnotationBinder( this ),
					new HbmBinder( this )
			};
		}

		final ArrayList<String> processedEntityNames = new ArrayList<String>();

		prepare( binders, metadataSources );
		bindIndependentMetadata( binders, metadataSources );
		bindTypeDependentMetadata( binders, metadataSources );
		bindMappingMetadata( binders, metadataSources, processedEntityNames );
		bindMappingDependentMetadata( binders, metadataSources );

		// todo : remove this by coordinated ordering of entity processing
		new EntityReferenceResolver( this ).resolve();
	}

	private void prepare(Binder[] binders, MetadataSources metadataSources) {
		for ( Binder binder : binders ) {
			binder.prepare( metadataSources );
		}
	}

	private void bindIndependentMetadata(Binder[] binders, MetadataSources metadataSources) {
		for ( Binder binder : binders ) {
			binder.bindIndependentMetadata( metadataSources );
		}
	}

	private void bindTypeDependentMetadata(Binder[] binders, MetadataSources metadataSources) {
		for ( Binder binder : binders ) {
			binder.bindTypeDependentMetadata( metadataSources );
		}
	}

	private void bindMappingMetadata(Binder[] binders, MetadataSources metadataSources, List<String> processedEntityNames) {
		for ( Binder binder : binders ) {
			binder.bindMappingMetadata( metadataSources, processedEntityNames );
		}
	}

	private void bindMappingDependentMetadata(Binder[] binders, MetadataSources metadataSources) {
		for ( Binder binder : binders ) {
			binder.bindMappingDependentMetadata( metadataSources );
		}
	}

	@Override
	public void addFetchProfile(FetchProfile profile) {
		if ( profile == null || profile.getName() == null ) {
			throw new IllegalArgumentException( "Fetch profile object or name is null: " + profile );
		}
		fetchProfiles.put( profile.getName(), profile );
	}

	@Override
	public void addFilterDefinition(FilterDefinition def) {
		if ( def == null || def.getFilterName() == null ) {
			throw new IllegalArgumentException( "Filter definition object or name is null: "  + def );
		}
		filterDefs.put( def.getFilterName(), def );
	}

	public Iterable<FilterDefinition> getFilterDefinitions() {
		return filterDefs.values();
	}

	@Override
	public void addIdGenerator(IdGenerator generator) {
		if ( generator == null || generator.getName() == null ) {
			throw new IllegalArgumentException( "ID generator object or name is null." );
		}
		idGenerators.put( generator.getName(), generator );
	}

	@Override
	public IdGenerator getIdGenerator(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid generator name" );
		}
		return idGenerators.get( name );
	}
	@Override
	public void registerIdentifierGenerator(String name, String generatorClassName) {
		 identifierGeneratorFactory.register( name, getClassForName( generatorClassName ) );
	}

	@Override
	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		if ( auxiliaryDatabaseObject == null ) {
			throw new IllegalArgumentException( "Auxiliary database object is null." );
		}
		auxiliaryDatabaseObjects.add( auxiliaryDatabaseObject );
	}

	@Override
	public void addNamedNativeQuery(NamedSQLQueryDefinition def) {
		if ( def == null || def.getName() == null ) {
			throw new IllegalArgumentException( "Named native query definition object or name is null: " + def.getQueryString() );
		}
		namedNativeQueryDefs.put( def.getName(), def );
	}

	public NamedSQLQueryDefinition getNamedNativeQuery(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid native query name" );
		}
		return namedNativeQueryDefs.get( name );
	}

	@Override
	public Iterable<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions() {
		return namedNativeQueryDefs.values();
	}

	@Override
	public void addNamedQuery(NamedQueryDefinition def) {
		if ( def == null || def.getName() == null ) {
			throw new IllegalArgumentException( "Named query definition object or name is null: " + def.getQueryString() );
		}
		namedQueryDefs.put( def.getName(), def );
	}

	public NamedQueryDefinition getNamedQuery(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid query name" );
		}
		return namedQueryDefs.get( name );
	}

	@Override
	public Iterable<NamedQueryDefinition> getNamedQueryDefinitions() {
		return namedQueryDefs.values();
	}

	@Override
	public void addResultSetMapping(ResultSetMappingDefinition resultSetMappingDefinition) {
		if ( resultSetMappingDefinition == null || resultSetMappingDefinition.getName() == null ) {
			throw new IllegalArgumentException( "Resultset mappping object or name is null: " + resultSetMappingDefinition );
		}
		resultSetMappings.put( resultSetMappingDefinition.getName(), resultSetMappingDefinition );
	}

	@Override
	public Iterable<ResultSetMappingDefinition> getResultSetMappingDefinitions() {
		return resultSetMappings.values();
	}

	@Override
	public void addTypeDefinition(TypeDef typeDef) {
		if ( typeDef == null || typeDef.getName() == null ) {
			throw new IllegalArgumentException( "Type definition object or name is null: " + typeDef.getTypeClass() );
		}
		final TypeDef previous = typeDefs.put( typeDef.getName(), typeDef );
		if ( previous != null ) {
			LOG.debugf( "Duplicate typedef name [%s] now -> %s", typeDef.getName(), typeDef.getTypeClass() );
		}
	}

	@Override
	public Iterable<TypeDef> getTypeDefinitions() {
		return typeDefs.values();
	}

	public TypeDef getTypeDef(String name) {
		return typeDefs.get( name );
	}

	private ClassLoaderService classLoaderService(){
		if(classLoaderService==null){
			classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		}
		return classLoaderService;
	}

	private Class getClassForName(String className) {
		return classLoaderService().classForName( className );
	}

	@Override
	public Options getOptions() {
		return options;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return sessionFactoryBuilder.buildSessionFactory();
	}

	@Override
	public BasicServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	public EntityBinding getEntityBinding(String entityName) {
		return entityBindingMap.get( entityName );
	}

	@Override
	public EntityBinding getRootEntityBinding(String entityName) {
		EntityBinding rootEntityBinding = rootEntityBindingMap.get( entityName );
		if ( rootEntityBinding == null ) {
			EntityBinding entityBinding = entityBindingMap.get( entityName );
			if ( entityBinding == null ) {
				throw new IllegalStateException( "Unknown entity binding: " + entityName );
			}
			if ( entityBinding.isRoot() ) {
				rootEntityBinding = entityBinding;
			}
			else {
				if ( entityBinding.getEntity().getSuperType() == null ) {
					throw new IllegalStateException( "Entity binding has no root: " + entityName );
				}
				rootEntityBinding = getRootEntityBinding( entityBinding.getEntity().getSuperType().getName() );
			}
			rootEntityBindingMap.put( entityName, rootEntityBinding );
		}
		return rootEntityBinding;
	}

	public Iterable<EntityBinding> getEntityBindings() {
		return entityBindingMap.values();
	}

	public void addEntity(EntityBinding entityBinding) {
		final String entityName = entityBinding.getEntity().getName();
		if ( entityBindingMap.containsKey( entityName ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, entityName );
		}
		entityBindingMap.put( entityName, entityBinding );
	}

	public PluralAttributeBinding getCollection(String collectionRole) {
		return collectionBindingMap.get( collectionRole );
	}

	@Override
	public Iterable<PluralAttributeBinding> getCollectionBindings() {
		return collectionBindingMap.values();
	}

	public void addCollection(PluralAttributeBinding pluralAttributeBinding) {
		final String owningEntityName = pluralAttributeBinding.getEntityBinding().getEntity().getName();
		final String attributeName = pluralAttributeBinding.getAttribute().getName();
		final String collectionRole = owningEntityName + '.' + attributeName;
		if ( collectionBindingMap.containsKey( collectionRole ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, collectionRole );
		}
		collectionBindingMap.put( collectionRole, pluralAttributeBinding );
	}

	public void addImport(String importName, String entityName) {
		if ( importName == null || entityName == null ) {
			throw new IllegalArgumentException( "Import name or entity name is null" );
		}
		LOG.trace( "Import: " + importName + " -> " + entityName );
		String old = imports.put( importName, entityName );
		if ( old != null ) {
			LOG.debug( "import name [" + importName + "] overrode previous [{" + old + "}]" );
		}
	}

	public Iterable<Map.Entry<String, String>> getImports() {
		return imports.entrySet();
	}

	public Iterable<FetchProfile> getFetchProfiles() {
		return fetchProfiles.values();
	}

	public TypeResolver getTypeResolver() {
		return typeResolver;
	}

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		return sessionFactoryBuilder;
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return options.getNamingStrategy();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	private final MetaAttributeContext globalMetaAttributeContext = new MetaAttributeContext();

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return globalMetaAttributeContext;
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return this;
	}

	@Override
	public ClassHolder getClassHolder(String className) {
		ClassHolder classHolder = classHoldersByName.get( className );
		if ( classHolder == null ) {
			classHolder = ClassHolderImpl.createDeferredClassHolder( className, this );
			classHoldersByName.put( className, classHolder );
		}
		return classHolder;
	}

	@Override
	public ClassHolder getLoadedClassHolder(Class clazz) {
		return getLoadedClassHolder( ReflectionHelper.resolveType( clazz ) );
	}

	@Override
	public ClassHolder getLoadedClassHolder(ResolvedType resolvedType) {
		ClassHolder classHolder = classHoldersByName.get( resolvedType.getErasedType().getName() );
		if ( classHolder == null ) {
			classHolder = ClassHolderImpl.createLoadedClassHolder( resolvedType, this );
			classHoldersByName.put( resolvedType.getErasedType().getName(), classHolder );
		}
		else if ( ! classHolder.isClassResolved() ) {
			// found the ClassHolder, but the class is not resolved; force resolution.
			Class loadedClass = classHolder.getLoadedClass();
			if ( resolvedType.getErasedType() != loadedClass ) {
				throw new AssertionFailure( "Class resolved from ClassHolder is different from the clazz in ResolvedType." );
			}
		}
		return classHolder;
	}

	/* package-protected */
	ResolvedType getResolvedType(String className) {
		 return ReflectionHelper.resolveType( getClassForName( className ) );
	}

	private static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	private static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";
	private static final String DEFAULT_CASCADE = "none";
	private static final String DEFAULT_PROPERTY_ACCESS = "property";

	@Override
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return identifierGeneratorFactory;
	}

	@Override
	public Type getIdentifierType(String entityName) throws MappingException {
		EntityBinding entityBinding = getEntityBinding( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}
		return entityBinding
				.getEntityIdentifier()
				.getValueBinding()
				.getHibernateTypeDescriptor()
				.getExplicitType();
	}

	@Override
	public String getIdentifierPropertyName(String entityName) throws MappingException {
		EntityBinding entityBinding = getEntityBinding( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}
		AttributeBinding idBinding = entityBinding.getEntityIdentifier().getValueBinding();
		return idBinding == null ? null : idBinding.getAttribute().getName();
	}

	@Override
	public Type getReferencedPropertyType(String entityName, String propertyName) throws MappingException {
		EntityBinding entityBinding = getEntityBinding( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}
		// TODO: should this call EntityBinding.getReferencedAttributeBindingString), which does not exist yet?
		AttributeBinding attributeBinding = entityBinding.getAttributeBinding( propertyName );
		if ( attributeBinding == null ) {
			throw new MappingException( "unknown property: " + entityName + '.' + propertyName );
		}
		return attributeBinding.getHibernateTypeDescriptor().getExplicitType();
	}

	private class MappingDefaultsImpl implements MappingDefaults {

		@Override
		public String getPackageName() {
			return null;
		}

		@Override
		public String getSchemaName() {
			return options.getDefaultSchemaName();
		}

		@Override
		public String getCatalogName() {
			return options.getDefaultCatalogName();
		}

		@Override
		public String getIdColumnName() {
			return DEFAULT_IDENTIFIER_COLUMN_NAME;
		}

		@Override
		public String getDiscriminatorColumnName() {
			return DEFAULT_DISCRIMINATOR_COLUMN_NAME;
		}

		@Override
		public String getCascadeStyle() {
			return DEFAULT_CASCADE;
		}

		@Override
		public String getPropertyAccessorName() {
			return DEFAULT_PROPERTY_ACCESS;
		}

		@Override
		public boolean areAssociationsLazy() {
			return true;
		}
	}
}
