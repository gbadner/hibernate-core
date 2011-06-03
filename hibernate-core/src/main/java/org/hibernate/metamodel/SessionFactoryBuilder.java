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
package org.hibernate.metamodel;

import javax.persistence.SharedCacheMode;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.NamingStrategy;

/**
 * @author Gail Badner
 */
public interface SessionFactoryBuilder {
	public SessionFactoryBuilder withCacheRegionPrefix(String cacheRegionPrefix);

	public SessionFactoryBuilder withMinimalPutsEnabled(boolean useMinimalPuts);

	public SessionFactoryBuilder withSecondLevelCacheEnabled(boolean enabled);

	public SessionFactoryBuilder withQueryCacheEnabled(boolean enabled);

	public SessionFactoryBuilder withAutoCreateSchemaEnabled(boolean enabled);

	public SessionFactoryBuilder withAutoDropSchemaEnabled(boolean enabled);

	public SessionFactoryBuilder withAutoUpdateSchemaEnabled(boolean enabled);

	public SessionFactoryBuilder withAutoValidateSchemaEnabled(boolean enabled);

	public SessionFactoryBuilder with(Interceptor interceptor);
	public SessionFactoryBuilder with(ConnectionReleaseMode connectionReleaseMode);
	public SessionFactoryBuilder with(EntityMode defaultEntityMode);
	public SessionFactoryBuilder withAutoCloseSessionEnabled(boolean enabled);
	public SessionFactoryBuilder withFlushBeforeCompletionEnabled(boolean enabled);

	public SessionFactoryBuilder with(QueryCacheFactory queryCacheFactory);
	public SessionFactoryBuilder withCheckNullabilityEnabled(boolean enabled);

	public SessionFactory buildSessionFactory();
}
