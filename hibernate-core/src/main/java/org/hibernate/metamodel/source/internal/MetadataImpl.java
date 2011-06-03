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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import org.hibernate.DuplicateMappingException;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.id.factory.DefaultIdentifierGeneratorFactory;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SourceProcessingOrder;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.relational.Database;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.metamodel.source.annotations.AnnotationBinder;
import org.hibernate.metamodel.source.annotations.xml.OrmXmlParser;
import org.hibernate.metamodel.source.hbm.HbmBinder;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
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
 */
public class MetadataImpl implements MetadataImplementor, Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MetadataImpl.class.getName()
	);

	private final BasicServiceRegistry serviceRegistry;
	private final Options options;
	private ClassLoaderService classLoaderService;
	private final Database database = new Database();
	private TypeResolver typeResolver = new TypeResolver();
	private DefaultIdentifierGeneratorFactory identifierGeneratorFactory = new DefaultIdentifierGeneratorFactory();
	/**
	 * Maps the fully qualified class name of an entity to its entity binding
	 */
	private Map<String, EntityBinding> entityBindingMap = new HashMap<String, EntityBinding>();
	private Map<String, PluralAttributeBinding> collectionBindingMap = new HashMap<String, PluralAttributeBinding>();
	private Map<String, FetchProfile> fetchProfiles = new HashMap<String, FetchProfile>();
	private Map<String, String> imports;
	private Map<String, TypeDef> typeDefs = new HashMap<String, TypeDef>();
	private Map<String, IdGenerator> idGenerators = new HashMap<String, IdGenerator>();
	private Map<String, NamedQueryDefinition> namedQueryDefs = new HashMap<String, NamedQueryDefinition>();
	private Map<String, NamedSQLQueryDefinition> namedNativeQueryDefs = new HashMap<String, NamedSQLQueryDefinition>();
	private Map<String, FilterDefinition> filterDefs = new HashMap<String, FilterDefinition>();

	public MetadataImpl(MetadataSources metadataSources, Options options) {
		this.serviceRegistry = metadataSources.getServiceRegistry();
		this.options = options;

		final ArrayList<String> processedEntityNames = new ArrayList<String>();
		if ( options.getSourceProcessingOrder() == SourceProcessingOrder.HBM_FIRST ) {
			applyHibernateMappings( metadataSources, processedEntityNames );
			applyAnnotationMappings( metadataSources, processedEntityNames );
		}
		else {
			applyAnnotationMappings( metadataSources, processedEntityNames );
			applyHibernateMappings( metadataSources, processedEntityNames );
		}

		new EntityReferenceResolver( this ).resolve();
	}

	@Override
	public void addFetchProfile(FetchProfile profile) {
		fetchProfiles.put( profile.getName(), profile );
	}

	@Override
	public void addFilterDefinition(FilterDefinition def) {
		filterDefs.put( def.getFilterName(), def );
	}

	public Iterable<FilterDefinition> getFilterDefinitions() {
		return filterDefs.values();
	}

	public void addIdGenerator(IdGenerator generator) {
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
		 identifierGeneratorFactory.register( name, classLoaderService().classForName( generatorClassName ) );
	}

	public void addNamedNativeQuery(String name, NamedSQLQueryDefinition def) {
		namedNativeQueryDefs.put( name, def );
	}

	public NamedSQLQueryDefinition getNamedNativeQuery(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid native query name" );
		}
		return namedNativeQueryDefs.get( name );
	}

	public void addNamedQuery(String name, NamedQueryDefinition def) {
		namedQueryDefs.put( name, def );
	}

	public NamedQueryDefinition getNamedQuery(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "null is not a valid query name" );
		}
		return namedQueryDefs.get( name );
	}

	@Override
	public void addTypeDef(TypeDef typeDef) {
		final TypeDef previous = typeDefs.put( typeDef.getName(), typeDef );
		if ( previous != null ) {
			LOG.debugf( "Duplicate typedef name [%s] now -> %s", typeDef.getName(), typeDef.getTypeClass() );
		}
	}

	@Override
	public Iterable<TypeDef> getTypeDefs() {
		return typeDefs.values();
	}

	public TypeDef getTypeDef(String name) {
		return typeDefs.get( name );
	}

	private void applyHibernateMappings(MetadataSources metadataSources, List<String> processedEntityNames) {
		for ( JaxbRoot jaxbRoot : metadataSources.getJaxbRootList() ) {
			// filter to just hbm-based roots
			if ( jaxbRoot.getRoot() instanceof XMLHibernateMapping ) {
				final HbmBinder mappingBinder = new HbmBinder(
						this, Collections.<String, MetaAttribute>emptyMap(), jaxbRoot
				);
				mappingBinder.processHibernateMapping();
			}
		}
	}

	private void applyAnnotationMappings(MetadataSources metadataSources, List<String> processedEntityNames) {
		// create a jandex index from the annotated classes
		Indexer indexer = new Indexer();
		for ( Class<?> clazz : metadataSources.getAnnotatedClasses() ) {
			indexClass( indexer, clazz.getName().replace( '.', '/' ) + ".class" );
		}

		// add package-info from the configured packages
		for ( String packageName : metadataSources.getAnnotatedPackages() ) {
			indexClass( indexer, packageName.replace( '.', '/' ) + "/package-info.class" );
		}
		Index index = indexer.complete();


		List<JaxbRoot<XMLEntityMappings>> mappings = new ArrayList<JaxbRoot<XMLEntityMappings>>();
		for ( JaxbRoot<?> root : metadataSources.getJaxbRootList() ) {
			if ( root.getRoot() instanceof XMLEntityMappings ) {
				mappings.add( (JaxbRoot<XMLEntityMappings>) root );
			}
		}
		if ( !mappings.isEmpty() ) {
			// process the xml configuration
			final OrmXmlParser ormParser = new OrmXmlParser( this );
			index = ormParser.parseAndUpdateIndex( mappings, index );
		}

		// create the annotation binder and pass it the final annotation index
		final AnnotationBinder annotationBinder = new AnnotationBinder( this, index );
		annotationBinder.bind();
	}

	/**
	 * Adds a single class to the jandex index
	 *
	 * @param indexer the jandex indexer
	 * @param className the fully qualified name of the class
	 */
	private void indexClass(Indexer indexer, String className) {
		InputStream stream = classLoaderService().locateResourceStream( className );
		try {
			indexer.index( stream );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to open input stream for class " + className, e );
		}
	}

	private ClassLoaderService classLoaderService(){
		if(classLoaderService==null){
			classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		}
		return classLoaderService;
	}

	@Override
	public Options getOptions() {
		return options;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		// todo : implement!!!!
		return null;
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

	public Iterable<PluralAttributeBinding> getCollections() {
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
		if ( imports == null ) {
			imports = new HashMap<String, String>();
		}
		LOG.trace( "Import: " + importName + " -> " + entityName );
		String old = imports.put( importName, entityName );
		if ( old != null ) {
			LOG.debug( "import name [" + importName + "] overrode previous [{" + old + "}]" );
		}
	}

	public Iterable<FetchProfile> getFetchProfiles() {
		return fetchProfiles.values();
	}

	public TypeResolver getTypeResolver() {
		return typeResolver;
	}

	@Override
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return identifierGeneratorFactory;
	}

	/**
	 * Returns the identifier type of a mapped class
	 */
	@Override
	public Type getIdentifierType(String entityName) throws MappingException {
		EntityBinding entityBinding = entityBindingMap.get( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}
		return entityBinding.getEntityIdentifier().getValueBinding().getHibernateTypeDescriptor().getExplicitType();
	}

	@Override
	public String getIdentifierPropertyName(String entityName) throws MappingException {
		final EntityBinding entityBinding = entityBindingMap.get( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}
		if ( entityBinding.getEntityIdentifier().getValueBinding() == null ) {
			return null;
		}
		return entityBinding.getEntityIdentifier().getValueBinding().getAttribute().getName();
	}

	@Override
	public Type getReferencedPropertyType(String entityName, String propertyName) throws MappingException {
		final EntityBinding entityBinding = entityBindingMap.get( entityName );
		if ( entityBinding == null ) {
			throw new MappingException( "Entity binding not known: " + entityName );
		}
		AttributeBinding prop = entityBinding.getAttributeBinding( propertyName );
		if ( prop == null ) {
			throw new MappingException(
					"property not known: " +
					entityName + '.' + propertyName
			);
		}
		return prop.getHibernateTypeDescriptor().getExplicitType();
	}
}
