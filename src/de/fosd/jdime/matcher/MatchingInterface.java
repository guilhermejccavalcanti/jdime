/*******************************************************************************
 * Copyright (C) 2013, 2014 Olaf Lessenich.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 *******************************************************************************/
package de.fosd.jdime.matcher;

import de.fosd.jdime.common.Artifact;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 */
public interface MatchingInterface<T extends Artifact<T>> {

	/**
	 * Returns a tree of matches for the provided artifacts.
	 *
	 * @param left
	 *            artifact
	 * @param right
	 *            artifact
	 * @return tree of matches
	 */
	Matching<T> match(final T left, final T right);
}
