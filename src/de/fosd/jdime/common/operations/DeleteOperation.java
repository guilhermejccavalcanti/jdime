/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package de.fosd.jdime.common.operations;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.DummyReport;
import de.fosd.jdime.common.MergeReport;

/**
 * @author Olaf Lessenich
 *
 */
public class DeleteOperation extends Operation {
	private Artifact artifact;
	
	public Artifact getArtifact() {
		return artifact;
	}

	public DeleteOperation(Artifact artifact) {
		this.artifact = artifact;
	}
		
	public String toString() {
		return "DELETE " + artifact.toString();
	}
	
	public String description() {
		return "Deleting " + artifact.toString();
	}
	
	public final MergeReport apply() {
		//TODO: create a real report
		System.out.println("IMPLEMENT ME: DeleteOperation.apply()");
		return new DummyReport();
	}
}