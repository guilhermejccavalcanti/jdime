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
package de.fosd.jdime.common.operations;

import java.io.IOException;

import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.LangElem;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.stats.ASTStats;
import de.fosd.jdime.stats.Stats;
import de.fosd.jdime.stats.StatsElement;

/**
 * The operation deletes <code>Artifact</code>s.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 *
 */
public class DeleteOperation<T extends Artifact<T>> extends Operation<T> {

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(DeleteOperation.class));

	/**
	 * The <code>Artifact</code> that is deleted by the operation.
	 */
	private T artifact;

	/**
	 * Class constructor.
	 *
	 * @param artifact
	 *            that is deleted by the operation
	 */
	public DeleteOperation(final T artifact) {
		super();
		this.artifact = artifact;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.operations.Operation#apply()
	 */
	@Override
	public final void apply(final MergeContext context) throws IOException {
		assert (artifact != null);
		assert (artifact.exists()) : "Artifact does not exist: " + artifact;

		if (LOG.isDebugEnabled()) {
			LOG.debug("Applying: " + this);
		}

		// This method does actually nothing!
		//
		// Why?
		// While merging, the target node is created with no children.
		// Therefore if a deletion of an element is applied during the merge,
		// nothing has to be done.
		//
		// For ASTNodeArtifacts, the important method we rely on here is
		// StructuredStrategy.merge(), which calls
		// ASTNodeArtifact.createProgram(ASTNodeArtifact artifact),
		// which then calls deleteChildren() on the created Program.

		if (context.hasStats()) {
			// but for the statistics, we have to look at the element
			Stats stats = context.getStats();
			stats.incrementOperation(this);
			StatsElement element = stats.getElement(artifact
					.getStatsKey(context));
			element.incrementDeleted();
			
			if (artifact instanceof FileArtifact) {

				// analyze java files to get statistics
				for (FileArtifact child : ((FileArtifact) artifact)
						.getJavaFiles()) {
					ASTNodeArtifact childAST = new ASTNodeArtifact(child);
					ASTStats childStats = childAST.getStats(null,
							LangElem.TOPLEVELNODE, false);
					if (LOG.isDebugEnabled()) {
						LOG.debug(childStats.toString());
					}
					
					childStats.setRemovalsfromAdditions(childStats);
					childStats.resetAdditions();

					if (context.isConsecutive()) {
						context.getStats().addRightStats(childStats);
					} else {
						context.getStats().addASTStats(childStats);
					}
				}
			}
		}
	}

	@Override
	public final String getName() {
		return "DELETE";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return getId() + ": " + getName() + " " + artifact;
	}
}
