/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cache.internal;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;

/**
 * Abstract class that will delegate all calls to org.hibernate.cache.spi.RegionFactory to the instance it wraps.
 * This abstracts the Singleton CacheManager construct of Ehcache
 *
 * @author Alex Snaps
 */
class AbstractEhCacheRegionFactory implements RegionFactory {

	private final RegionFactory underlyingRegionFactory;

	/**
	 * {@inheritDoc}
	 */
	protected AbstractEhCacheRegionFactory(RegionFactory regionFactory) {
		underlyingRegionFactory = regionFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void start(final org.hibernate.cfg.Settings settings, final Properties properties) throws CacheException {
		underlyingRegionFactory.start(settings, properties);
	}

	/**
	 * {@inheritDoc}
	 */
	public final void start(final SessionFactory.Settings options, final Properties properties) throws CacheException {
		underlyingRegionFactory.start(options, properties);
	}

	/**
	 * {@inheritDoc}
	 */
	public final void stop() {
		underlyingRegionFactory.stop();
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean isMinimalPutsEnabledByDefault() {
		return underlyingRegionFactory.isMinimalPutsEnabledByDefault();
	}

	/**
	 * {@inheritDoc}
	 */
	public final AccessType getDefaultAccessType() {
		return underlyingRegionFactory.getDefaultAccessType();
	}

	/**
	 * {@inheritDoc}
	 */
	public final long nextTimestamp() {
		return underlyingRegionFactory.nextTimestamp();
	}

	/**
	 * {@inheritDoc}
	 */
	public final EntityRegion buildEntityRegion(final String regionName, final Properties properties, final CacheDataDescription metadata) throws CacheException {
		return underlyingRegionFactory.buildEntityRegion(regionName, properties, metadata);
	}

	/**
	 * {@inheritDoc}
	 */
	public final CollectionRegion buildCollectionRegion(final String regionName, final Properties properties, final CacheDataDescription metadata) throws CacheException {
		return underlyingRegionFactory.buildCollectionRegion(regionName, properties, metadata);
	}

	/**
	 * {@inheritDoc}
	 */
	public final QueryResultsRegion buildQueryResultsRegion(final String regionName, final Properties properties) throws CacheException {
		return underlyingRegionFactory.buildQueryResultsRegion(regionName, properties);
	}

	/**
	 * {@inheritDoc}
	 */
	public final TimestampsRegion buildTimestampsRegion(final String regionName, final Properties properties) throws CacheException {
		return underlyingRegionFactory.buildTimestampsRegion(regionName, properties);
	}
}
