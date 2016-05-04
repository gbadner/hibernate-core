/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.compare;

import java.lang.reflect.Array;
import java.util.Comparator;

/**
 * @author Gail Badner
 */
public final class PrimitiveByteArrayComparator implements Comparator<byte[]> {
	public static final Comparator<byte[]> INSTANCE = new PrimitiveByteArrayComparator();

	private PrimitiveByteArrayComparator() {
	}

	@Override
	public int compare(byte[] o1, byte[] o2) {
		final int lengthToCheck = Math.min( Array.getLength( o1 ), Array.getLength( o2 ) );
		for ( int i = 0 ; i < lengthToCheck ; i++ ) {
			int comparison = o1[i] - o2[i];
			if ( comparison != 0 ) {
				return comparison;
			}
		}
		return Array.getLength( o1 ) - Array.getLength( o2 );
	}
}