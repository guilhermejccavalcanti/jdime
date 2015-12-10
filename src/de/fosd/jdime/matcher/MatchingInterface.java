/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
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
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 */
package de.fosd.jdime.matcher;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.merge.MergeInterface;

/**
 * This interface should be implemented by <code>Matcher</code> classes that compare <code>Artifact</code>s and compute
 * <code>Matching</code>s.
 * <p>
 * Based on the computed <code>Matchings</code>, the <code>Merge</code> implementation (see {@link MergeInterface})
 * amalgamates a new, unified <code>Artifact</code>.
 *
 * @param <T>
 *         type of <code>Artifact</code>
 * @author Olaf Lessenich
 */
public interface MatchingInterface<T extends Artifact<T>> {

    /**
     * Returns a <code>Set</code> of <code>Matching</code>s for the provided <code>Artifact</code>s.
     *
     * @param context
     *         the <code>MergeContext</code> of the merge operation
     * @param left
     *         the left <code>Artifact</code> to compare
     * @param right
     *         the right <code>Artifact</code> to compare
     * @param lookAhead
     *         How many levels to keep searching for matches in the subtree if the currently compared nodes are not equal. If
     *         there are no matches within the specified number of levels, do not look for matches deeper in the subtree. If
     *         this is set to LOOKAHEAD_OFF, the matcher will stop looking for subtree matches if two nodes do not match. If
     *         this is set to LOOKAHEAD_FULL, the matcher will look at the entire subtree.  The default ist to do no
     *         look-ahead matching.
     * @return a <code>Set</code> of <code>Matching</code>s
     */
    Matchings<T> match(MergeContext context, T left, T right, int lookAhead);
}
