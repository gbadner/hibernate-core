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
package org.hibernate.metamodel.source.spi;

import java.util.Map;

import org.hibernate.Interceptor;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.relational.Database;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.type.TypeResolver;

/**
 * @author Steve Ebersole
 */
public interface MetadataImplementor extends Metadata, Mapping {
	public BasicServiceRegistry getServiceRegistry();

	public Database getDatabase();

	public Iterable<EntityBinding> getEntityBindings();

	public EntityBinding getEntityBinding(String entityName);

	public TypeResolver getTypeResolver();

	public void addImport(String entityName, String entityName1);

	public Map<String, String> getImports();

	public void addEntity(EntityBinding entityBinding);

	public void addCollection(PluralAttributeBinding collectionBinding);

	public Iterable<PluralAttributeBinding> getCollections();

	public void addFetchProfile(FetchProfile profile);

	public Iterable<FetchProfile> getFetchProfiles();

	public void addTypeDefinition(TypeDef typeDef);

	public Iterable<TypeDef> getTypeDefinitions();

	public void addFilterDefinition(FilterDefinition filterDefinition);

	public Iterable<FilterDefinition> getFilterDefinitions();

	public void registerIdentifierGenerator(String name, String clazz);

	public IdGenerator getIdGenerator(String name);

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	public Map<String, NamedQueryDefinition> getNamedQueries();

	public Map<String, NamedSQLQueryDefinition> getNamedSqlQueries();

	public Map<String, ResultSetMappingDefinition> getSqlResultSetMappings();

	public Map<String, SQLFunction> getSqlFunctions();

	public Interceptor getInterceptor();

	EntityNotFoundDelegate getEntityNotFoundDelegate();
}
