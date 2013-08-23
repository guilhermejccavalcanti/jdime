/**
 * 
 */
package de.fosd.jdime.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.log4j.Logger;

import AST.ASTNode;
import AST.BytecodeParser;
import AST.CompilationUnit;
import AST.ConstructorDecl;
import AST.FieldDecl;
import AST.FieldDeclaration;
import AST.ImportDecl;
import AST.InterfaceDecl;
import AST.JavaParser;
import AST.MethodDecl;
import AST.Program;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.strategy.ASTNodeStrategy;
import de.fosd.jdime.strategy.MergeStrategy;

/**
 * @author Olaf Lessenich
 * 
 */
public class ASTNodeArtifact extends Artifact<ASTNodeArtifact> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(ASTNodeArtifact.class);

	/**
	 * Encapsulated ASTNode.
	 */
	private ASTNode<?> astnode = null;

	/**
	 * 
	 */
	private int number = -1;

	/**
	 * 
	 */
	private static int count = 1;

	/**
	 * Initializes a program.
	 * 
	 * @return program
	 */
	private static Program initProgram() {
		Program program = new Program();
		program.state().reset();
		program.initBytecodeReader(new BytecodeParser());
		initParser(program);
		return program;
	}

	/**
	 * Initializes parser.
	 * 
	 * @param p
	 *            program
	 */
	private static void initParser(final Program p) {
		p.initJavaParser(new JavaParser() {

			@Override
			public CompilationUnit parse(final java.io.InputStream is,
					final String fileName) throws java.io.IOException,
					beaver.Parser.Exception {
				return new parser.JavaParser().parse(is, fileName);
			}
		});
	}

	/**
	 * Constructor class.
	 */
	public ASTNodeArtifact() {

	}

	/**
	 * Constructs an ASTNodeArtifact from a FileArtifact.
	 * 
	 * @param artifact
	 *            file artifact
	 */
	public ASTNodeArtifact(final FileArtifact artifact) {
		assert (artifact != null);

		setRevision(artifact.getRevision());

		ASTNode<?> astnode = null;
		if (artifact.isEmptyDummy()) {
			astnode = new ASTNode<>();
			setEmptyDummy(true);
		} else {
			Program p = initProgram();
			p.addSourceFile(artifact.getPath());
			astnode = p;
		}

		assert (astnode != null);
		this.astnode = astnode;
		count = 1;
		renumber(this);

	}

	/**
	 * Recursivley renumbers the node.
	 * 
	 * @param artifact
	 *            node to renumber
	 */
	private static void renumber(final ASTNodeArtifact artifact) {
		artifact.number = count;
		count++;
		for (int i = 0; i < artifact.getNumChildren(); i++) {
			renumber(artifact.getChild(i));
		}
	}

	/**
	 * @param astnode
	 *            astnode
	 */
	public ASTNodeArtifact(final ASTNode<?> astnode) {
		assert (astnode != null);
		this.astnode = astnode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.common.Artifact#addChild(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final ASTNodeArtifact addChild(final ASTNodeArtifact child)
			throws IOException {
		ASTNodeArtifact myChild;
		try {
			myChild = new ASTNodeArtifact((ASTNode<?>) child.astnode.clone());
			myChild.deleteChildren();
			myChild.setRevision(child.getRevision());
			myChild.setParent(this);
			myChild.astnode.setParent(astnode);
			myChild.setRevision(child.getRevision());
			myChild.number = child.number;
			myChild.cloneMatches(child);

			if (children == null) {
				initializeChildren();
			}
			children.add(myChild);
			astnode.flushCaches();
			if (LOG.isTraceEnabled()) {
				LOG.trace("Added child " + myChild.getId() + " to parent node "
						+ getId());
			}
			return myChild;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new NotYetImplementedException();
		}

	}

	/**
	 * 
	 */
	public final void deleteChildren() {
		while (hasChildren()) {
			ASTNodeArtifact child = getChild(0);
			child.astnode = null;
			child = null;
			children.remove(0);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.common.Artifact#copyArtifact(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final void copyArtifact(final ASTNodeArtifact destination)
			throws IOException {

		ASTNodeArtifact copy = destination.addChild(this);
		if (hasChildren()) {
			for (ASTNodeArtifact child : getChildren()) {
				child.copyArtifact(copy);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#createArtifact(boolean)
	 */
	@Override
	public final void createArtifact(final boolean isLeaf) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#createEmptyDummy()
	 */
	@Override
	public final ASTNodeArtifact createEmptyDummy()
			throws FileNotFoundException {
		ASTNodeArtifact dummy = new ASTNodeArtifact();
		dummy.astnode = new ASTNode<>();
		dummy.setEmptyDummy(true);
		dummy.setRevision(getRevision());
		return dummy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(final Object obj) {
		assert (astnode != null);
		assert (obj != null);
		assert (obj instanceof ASTNodeArtifact);
		return astnode.dumpString().equals(
				((ASTNodeArtifact) obj).astnode.dumpString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#exists()
	 */
	@Override
	public final boolean exists() {
		return astnode != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#getId()
	 */
	@Override
	public final String getId() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#getName()
	 */
	@Override
	public final String getName() {
		return getRevision().toString() + ":" + number;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#hashCode()
	 */
	@Override
	public final int hashCode() {
		return astnode.dumpString().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#initializeChildren()
	 */
	@Override
	public final void initializeChildren() {
		assert (astnode != null);
		ArtifactList<ASTNodeArtifact> children = new ArtifactList<ASTNodeArtifact>();
		for (int i = 0; i < astnode.getNumChild(); i++) {
			ASTNodeArtifact child = new ASTNodeArtifact(astnode.getChild(i));
			child.setParent(this);
			child.setRevision(getRevision());
			children.add(child);
		}
		setChildren(children);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#isLeaf()
	 */
	@Override
	public final boolean isLeaf() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#merge(de.fosd.jdime.common.operations.
	 * MergeOperation, de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(final MergeOperation<ASTNodeArtifact> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		assert (operation != null);
		assert (context != null);

		MergeStrategy<ASTNodeArtifact> strategy = new ASTNodeStrategy();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Using strategy: " + strategy.toString());
		}

		strategy.merge(operation, context);

		if (!context.isQuiet()) {
			System.out.println(context.getStdIn());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#write(java.lang.String)
	 */
	@Override
	public final void write(final String str) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public final String getStatsKey(final MergeContext context) {
		return "nodes";
	}

	public final String dumpRootTree() {
		if (getParent() != null) {
			return getParent().dumpRootTree();
		} else {
			return dumpTree();
		}
	}

	/**
	 * Returns the AST as indented plain text, as provided by JastAddJ.
	 * 
	 * @return AST as indented plain text
	 */
	public final String dumpTree() {
		assert (astnode != null);
		return dumpTree("");
	}

	/**
	 * @param indent
	 *            String used to indent the current node
	 * @return ASCII String representing the tree
	 */
	private String dumpTree(final String indent) {
		StringBuffer sb = new StringBuffer();

		// node itself

		Matching<ASTNodeArtifact> m = null;
		if (hasMatches()) {

			Set<Revision> matchingRevisions = matches.keySet();

			// print color code
			String color = "";

			for (Revision rev : matchingRevisions) {
				m = getMatching(rev);
				color = m.getColor().toShell();
			}

			sb.append(color);
		}

		sb.append(indent + "(" + getId() + ") ");
		sb.append(astnode.dumpString());

		if (hasMatches()) {
			assert (m != null);
			sb.append(" <=> (" + m.getMatchingArtifact(this).getId() + ")");
			sb.append(Color.DEFAULT.toShell());
		}
		sb.append(System.lineSeparator());

		// children
		for (ASTNodeArtifact child : getChildren()) {
			sb.append(child.dumpTree(indent + "  "));
		}

		return sb.toString();
	}

	/**
	 * Returns the AST in dot-format.
	 * 
	 * @param includeNumbers
	 *            include node number in label if true
	 * @return AST in dot-format.
	 */
	public final String dumpGraphvizTree(final boolean includeNumbers) {
		assert (astnode != null);
		StringBuffer sb = new StringBuffer();
		sb.append(number + "[label=\"");

		// node label
		if (includeNumbers) {
			sb.append("(" + number + ") ");
		}

		sb.append(astnode.dumpString());

		sb.append("\"");

		if (hasMatches()) {
			sb.append(", fillcolor = green, style = filled");
		}

		sb.append("];");
		sb.append(System.lineSeparator());

		// children
		for (ASTNodeArtifact child : getChildren()) {
			sb.append(child.dumpGraphvizTree(includeNumbers));

			// edge
			sb.append(number + "->" + child.number + ";"
					+ System.lineSeparator());
		}

		return sb.toString();
	}

	/**
	 * Returns whether declaration order is significant for this node.
	 * 
	 * @return whether declaration order is significant for this node
	 */
	public final boolean isOrdered() {
		if (astnode instanceof CompilationUnit
				|| astnode instanceof ConstructorDecl
				|| astnode instanceof MethodDecl
				|| astnode instanceof InterfaceDecl
				|| astnode instanceof FieldDecl
				|| astnode instanceof FieldDeclaration
				|| astnode instanceof ImportDecl) {
			return false;
		}

		return true;
	}

	@Override
	public final boolean hasUniqueLabels() {
		return false;
	}

	/**
	 * Returns whether a node matches another node.
	 * 
	 * @param other
	 *            node to compare with.
	 * @return true if the node matches another node.
	 */
	public final boolean matches(final ASTNodeArtifact other) {
		assert (astnode != null);
		assert (other != null);
		assert (other.astnode != null);

		return astnode.dumpString().equals(other.astnode.dumpString());
	}

	/**
	 * Returns the size of the subtree. The node itself is not included.
	 * 
	 * @return size of subtree
	 */
	public final int getSubtreeSize() {
		int size = getNumChildren();

		for (int i = 0; i < getNumChildren(); i++) {
			size += getChild(i).getSubtreeSize();
		}

		return size;
	}

	/**
	 * Returns the size of the tree. The node itself is also included.
	 * 
	 * @return size of tree
	 */
	public final int getTreeSize() {
		return getSubtreeSize() + 1;
	}

	/**
	 * Force renumbering of the tree.
	 */
	public final void forceRenumbering() {
		count = 1;
		renumber(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#isEmpty()
	 */
	@Override
	public final boolean isEmpty() {
		return !hasChildren();
	}

	@Override
	public final int compareTo(final ASTNodeArtifact o) {
		if (hasUniqueLabels()) {
			return astnode.dumpString().compareTo(o.astnode.dumpString());
		} else {
			throw new RuntimeException("This artifact is not comparable.");
		}
	}

	private void cloneMatches(ASTNodeArtifact other) {
		if (other.matches != null) {
			matches = new LinkedHashMap<Revision, Matching<ASTNodeArtifact>>();

			Set<Revision> matchingRevisions = other.matches.keySet();

			for (Revision rev : matchingRevisions) {
				Matching<ASTNodeArtifact> m = (Matching<ASTNodeArtifact>) other.matches
						.get(rev).clone();
				m.updateMatching(this);
				matches.put(rev, m);
			}
		}
	}
	
	/**
	 * Rebuild the encapsulated ASTNode tree top down.
	 * This should be only called at the root node
	 */
	public void rebuildAST() {
		ASTNode<?>[] newchildren = new ASTNode[getNumChildren()];

		for (int i = 0; i < getNumChildren(); i++) {
			ASTNodeArtifact child = getChild(i);
			newchildren[i] = child.astnode;
			newchildren[i].setParent(astnode);
			child.rebuildAST();
		}

		astnode.setChildren(newchildren);
		astnode.flushCache();
	}


	public final String prettyPrint() {
		assert (astnode != null);
		rebuildAST();
		astnode.flushCaches();
		System.out.println(astnode.dumpTree());
		return astnode.toString();
	}
	
	

}
