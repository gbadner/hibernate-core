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
package org.hibernate.metamodel.source.internal;

import org.jboss.logging.Logger;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.config.spi.ConfigurationService;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryBuilderImpl implements SessionFactoryBuilder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
				CoreMessageLogger.class, SessionFactoryBuilderImpl.class.getName()
	);

	private final MetadataImplementor metadata;
	private final OptionsImpl options;

	public SessionFactoryBuilderImpl(MetadataImplementor metadata) {
		this.metadata = metadata;
		this.options = new OptionsImpl( metadata.getServiceRegistry() );
	}

	@Override
	public SessionFactoryBuilder withCacheRegionPrefix(String cacheRegionPrefix) {
		this.options.cacheRegionPrefix = cacheRegionPrefix;
		return this;
	}

	@Override
	public SessionFactoryBuilder withSecondLevelCacheEnabled(boolean enabled) {
		this.options.useSecondLevelCache = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder withQueryCacheEnabled(boolean enabled) {
		this.options.useQueryCache = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder withMinimalPutsEnabled(boolean enabled) {
		this.options.useMinimalPuts = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder withAutoCreateSchemaEnabled(boolean enabled) {
		this.options.autoCreateSchema = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder withAutoDropSchemaEnabled(boolean enabled) {
		this.options.autoDropSchema = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder withAutoUpdateSchemaEnabled(boolean enabled) {
		this.options.autoUpdateSchema = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder withAutoValidateSchemaEnabled(boolean enabled) {
		this.options.autoValidateSchema = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder with(Interceptor interceptor) {
		this.options.interceptor = interceptor;
		return this;
	}

	@Override
	public SessionFactoryBuilder with(ConnectionReleaseMode connectionReleaseMode) {
		this.options.connectionReleaseMode = connectionReleaseMode;
		return this;
	}

	@Override
	public SessionFactoryBuilder with(EntityMode defaultEntityMode) {
		this.options.defaultEntityMode = defaultEntityMode;
		return this;
	}

	@Override
	public SessionFactoryBuilder withAutoCloseSessionEnabled(boolean enabled) {
		this.options.autoCloseSession = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder withFlushBeforeCompletionEnabled(boolean enabled) {
		this.options.flushBeforeCompletion = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder with(QueryCacheFactory queryCacheFactory) {
		this.options.queryCacheFactory = queryCacheFactory;
		return this;
	}

	@Override
	public SessionFactoryBuilder withCheckNullabilityEnabled(boolean enabled) {
		this.options.checkNullability = enabled;
		return this;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return new SessionFactoryImpl( metadata, options, null );
	}

	private static class OptionsImpl implements SessionFactory.Settings {
		private static final String DEFAULT_QUERY_CACHE_FACTORY =  "org.hibernate.cache.internal.StandardQueryCacheFactory";


		private String cacheRegionPrefix;
		private boolean useSecondLevelCache;
		private boolean useQueryCache;
		private boolean useMinimalPuts;
		private boolean autoCreateSchema;
		private boolean autoDropSchema;
		private boolean autoUpdateSchema;
		private boolean autoValidateSchema;
		private Interceptor interceptor;
		private ConnectionReleaseMode connectionReleaseMode;
		private EntityMode defaultEntityMode;
		private boolean autoCloseSession;
		private boolean flushBeforeCompletion;
		private QueryCacheFactory queryCacheFactory;
		private Boolean checkNullability;

		@Override
		public boolean isQueryCacheEnabled() {
			return useQueryCache;
		}

		public OptionsImpl(BasicServiceRegistry serviceRegistry) {
			ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );

			cacheRegionPrefix = configurationService.getSetting(
					AvailableSettings.CACHE_REGION_PREFIX,
					new ConfigurationService.Converter<String>() {
						@Override
						public String convert(Object value) {
							StringBuilder builder = new StringBuilder();
							if ( value != null ) {
								builder.append( String.valueOf( value ) ).append( '.' );
							}
							return builder.toString();
						}
					}
			);

			RegionFactory regionFactory = serviceRegistry.getService( RegionFactory.class );
			useMinimalPuts = configurationService.getSetting(
					AvailableSettings.USE_MINIMAL_PUTS,
					getBooleanConverter(),
					regionFactory.isMinimalPutsEnabledByDefault()
			);

			useSecondLevelCache = configurationService.getSetting(
					 AvailableSettings.USE_SECOND_LEVEL_CACHE,
					getBooleanConverter(),
					true
			);

			useQueryCache = configurationService.getSetting(
					AvailableSettings.USE_QUERY_CACHE,
					getBooleanConverter(),
					false
			);

			//Schema export:
			String autoSchemaExport =  configurationService.getSetting(
					AvailableSettings.HBM2DDL_AUTO,
					getStringConverter()
			);
			if ( "validate".equals(autoSchemaExport) ) {
				autoValidateSchema = true;
			}
			if ( "update".equals(autoSchemaExport) ) {
				autoUpdateSchema = true;
			}
			if ( "create".equals(autoSchemaExport) ) {
				autoCreateSchema = true;
			}
			if ( "create-drop".equals( autoSchemaExport ) ) {
				autoCreateSchema = true;
				autoDropSchema = true;
			}

			interceptor = EmptyInterceptor.INSTANCE;

			String releaseModeName = configurationService.getSetting(
					AvailableSettings.RELEASE_CONNECTIONS,
					getStringConverter(),
					"auto"
			);
			if ( "auto".equals(releaseModeName) ) {
				connectionReleaseMode = serviceRegistry.getService( TransactionFactory.class ).getDefaultReleaseMode();
			}
			else {
				connectionReleaseMode = ConnectionReleaseMode.parse( releaseModeName );
				if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT &&
						! serviceRegistry.getService( JdbcServices.class ).getConnectionProvider().supportsAggressiveRelease() ) {
					LOG.unsupportedAfterStatement();
					connectionReleaseMode = ConnectionReleaseMode.AFTER_TRANSACTION;
				}
			}

			String queryCacheFactoryClassName = configurationService.getSetting(
					AvailableSettings.QUERY_CACHE_FACTORY,
					getStringConverter(),
					DEFAULT_QUERY_CACHE_FACTORY
			);
			LOG.debugf( "Query cache factory: %s", queryCacheFactoryClassName );
			try {
				queryCacheFactory =
						(QueryCacheFactory) serviceRegistry.getService( ClassLoaderService.class )
								.classForName( queryCacheFactoryClassName )
								.newInstance();
			}
			catch (Exception e) {
				throw new HibernateException( "could not instantiate QueryCacheFactory: " + queryCacheFactoryClassName, e );
			}

			checkNullability = configurationService.getSetting(
					Environment.CHECK_NULLABILITY,
					getBooleanConverter()
			);
		}

		private static ConfigurationService.Converter<Boolean> getBooleanConverter() {
			return new ConfigurationService.Converter<Boolean>() {
				@Override
				public Boolean convert(Object value) {
					return Boolean.parseBoolean( value.toString() );
				}
			};
		}

		private static ConfigurationService.Converter<String> getStringConverter() {
			return new ConfigurationService.Converter<String>() {
				@Override
				public String convert(Object value) {
					return value.toString();
				}
			};
		}

		@Override
		public String getCacheRegionPrefix() {
			return cacheRegionPrefix;
		}

		@Override
		public boolean isSecondLevelCacheEnabled() {
			return useSecondLevelCache;
		}

		@Override
		public boolean isMinimalPutsEnabled() {
			return useMinimalPuts;
		}

		@Override
		public boolean isAutoValidateSchemaEnabled() {
			return autoValidateSchema;
		}

		@Override
		public boolean isAutoCreateSchemaEnabled() {
			return autoCreateSchema;
		}

		@Override
		public boolean isAutoDropSchemaEnabled() {
			return autoDropSchema;
		}

		@Override
		public boolean isAutoUpdateSchemaEnabled() {
			return autoUpdateSchema;
		}

		@Override
		public Interceptor getInterceptor() {
			return interceptor;
		}

		@Override
		public ConnectionReleaseMode getConnectionReleaseMode() {
			return connectionReleaseMode;
		}

		@Override
		public EntityMode getDefaultEntityMode() {
			return defaultEntityMode;
		}

		@Override
		public boolean isAutoCloseSessionEnabled() {
			return autoCloseSession;
		}

		@Override
		public boolean isFlushBeforeCompletionEnabled() {
			return flushBeforeCompletion;
		}

		@Override
		public QueryCacheFactory getQueryCacheFactory() {
			return queryCacheFactory;
		}

		@Override
		public boolean isCheckNullabilityEnabled() {
			return checkNullability;
		}

		@Override
		public void overrideCheckNullability(boolean enabled) {
			// LOG!!
			checkNullability = enabled;
		}
	}
}
