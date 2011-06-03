/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.internal.bridge;

import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.Cache;
import org.hibernate.cache.spi.QueryResultsRegion;

/**
 * Adapter specifically briding {@link org.hibernate.cache.spi.QueryResultsRegion} to {@link Cache}.
*
* @author Steve Ebersole
 */
public class QueryResultsRegionAdapter extends BaseGeneralDataRegionAdapter implements QueryResultsRegion {
	protected QueryResultsRegionAdapter(Cache underlyingCache, org.hibernate.cfg.Settings settings) {
		super( underlyingCache, settings );
	}

	protected QueryResultsRegionAdapter(Cache underlyingCache, SessionFactory.Settings options) {
		super( underlyingCache, options );
	}

}
