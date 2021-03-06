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

import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;
import de.fosd.jdime.matcher.ordered.SimpleTreeMatcher;
import de.fosd.jdime.matcher.unordered.LPMatcher;
import de.fosd.jdime.matcher.unordered.UniqueLabelMatcher;
import de.fosd.jdime.matcher.unordered.UnorderedMatcher;

/**
 * A <code>Matcher</code> is used to compare two <code>Artifacts</code> and to
 * compute and store <code>Matching</code>s.
 * <p>
 * The computation of <code>Matching</code>s is done recursively. Depending on
 * the <code>Artifact</code>, the matcher decides whether the order of elements
 * is important (e.g., statements within a method in a Java AST) or not (e.g.,
 * method declarations in a Java AST) for syntactic correctness. Then either an
 * implementation of <code>OrderedMatcher</code> or
 * <code>UnorderedMatcher</code> is called to compute the actual Matching.
 * Usually, those subclass implementations use this <code>Matcher</code>
 * superclass for the recursive call of the match() method.
 * <p>
 * When the computation is done and the best combination of matches have been
 * selected, they are stored recursively within the <code>Artifact</code> nodes
 * themselves, assigning each matched <code>Artifact</code> a pointer to the
 * corresponding matching <code>Artifact</code>.
 *
 * @author Olaf Lessenich
 *
 * @param <T> type of <code>Artifact</code>
 */
public class Matcher<T extends Artifact<T>> implements MatchingInterface<T> {

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(Matcher.class));
	private int calls = 0;
	private int orderedCalls = 0;
	private int unorderedCalls = 0;
	private UnorderedMatcher<T> unorderedMatcher;
	private UnorderedMatcher<T> unorderedLabelMatcher;
	private OrderedMatcher<T> orderedMatcher;

	public Matcher() {
		unorderedMatcher = new LPMatcher<>(this);
		unorderedLabelMatcher = new UniqueLabelMatcher<>(this);
		orderedMatcher = new SimpleTreeMatcher<>(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public final Matching<T> match(final T left, final T right) {
		boolean isOrdered = false;
		boolean uniqueLabels = true;

		for (int i = 0; !isOrdered && i < left.getNumChildren(); i++) {
			T leftChild = left.getChild(i);
			if (leftChild.isOrdered()) {
				isOrdered = true;
			}
			if (!uniqueLabels || !leftChild.hasUniqueLabels()) {
				uniqueLabels = false;
			}
		}

		for (int i = 0; !isOrdered && i < right.getNumChildren(); i++) {
			T rightChild = right.getChild(i);
			if (rightChild.isOrdered()) {
				isOrdered = true;
			}
			if (!uniqueLabels || !rightChild.hasUniqueLabels()) {
				uniqueLabels = false;
			}
		}

		calls++;

		if (isOrdered) {
			orderedCalls++;
			if (LOG.isTraceEnabled()) {
				LOG.trace(orderedMatcher.getClass().getSimpleName() + ".match("
						+ left.getId() + ", " + right.getId() + ")");
			}

			return orderedMatcher.match(left, right);
		} else {
			unorderedCalls++;

			if (uniqueLabels) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(unorderedLabelMatcher.getClass().getSimpleName()
							+ ".match(" + left.getId() + ", " + right.getId()
							+ ")");
				}

				return unorderedLabelMatcher.match(left, right);
			} else {
				if (LOG.isTraceEnabled()) {
					LOG.trace(unorderedMatcher.getClass().getSimpleName()
							+ ".match(" + left.getId() + ", " + right.getId()
							+ ")");
				}

				return unorderedMatcher.match(left, right);
			}
		}
	}

	/**
	 * Recursively marks corresponding nodes using an already computed matching.
	 * The respective nodes are flagged with an appropriate
	 * <code>matchingFlag</code> and references are set to each other.
	 *
	 * @param matching used to mark nodes
	 * @param color color used to highlight the matching in debug output
	 */
	public final void storeMatching(final Matching<T> matching,
			final Color color) {
		T left = matching.getLeft();
		T right = matching.getRight();

		if (matching.getScore() > 0) {
			assert (left.matches(right)) : left.getId() + " does not match "
					+ right.getId() + " (" + left + " <-> " + right + ")";
			matching.setColor(color);
			left.addMatching(matching);
			right.addMatching(matching);
			for (Matching<T> childMatching : matching.getChildren()) {
				storeMatching(childMatching, color);
			}
		}
	}

	/**
	 * Resets the call counter.
	 */
	public final void reset() {
		calls = 0;
		unorderedCalls = 0;
		orderedCalls = 0;
	}

	/**
	 * Returns the logged call counts.
	 *
	 * @return logged call counts
	 */
	public final String getLog() {
		StringBuilder sb = new StringBuilder();
		sb.append("matcher calls (all/ordered/unordered): ");
		sb.append(calls).append("/");
		sb.append(orderedCalls).append("/");
		sb.append(unorderedCalls);
		assert (calls == unorderedCalls + orderedCalls) : "Wrong sum for matcher calls";
		return sb.toString();
	}
}
