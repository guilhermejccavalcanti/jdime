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
package de.fosd.jdime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.stats.StatsPrinter;
import de.fosd.jdime.strategy.MergeStrategy;
import de.fosd.jdime.strategy.StrategyNotFoundException;

/**
 * @author Olaf Lessenich
 * 
 */
public final class Main {

	/**
	 * Private constructor.
	 */
	private Main() {

	}

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Main.class);

	/**
	 * Toolname constant.
	 */
	private static final String TOOLNAME = "jdime";

	/**
	 * Version constant.
	 */
	private static final double VERSION = 0.1;

	/**
	 * Perform a merge operation on the input files or directories.
	 * 
	 * @param args
	 *            command line arguments
	 * @throws IOException
	 *             If an input or output exception occurs
	 * @throws InterruptedException
	 *             If a thread is interrupted
	 */
	public static void main(final String[] args) throws IOException,
			InterruptedException {
		BasicConfigurator.configure();
		MergeContext context = new MergeContext();

		setLogLevel("INFO");
		LOG.debug("starting program");

		parseCommandLineArgs(context, args);
		ArtifactList<FileArtifact> inputFiles = context.getInputFiles();
		FileArtifact output = context.getOutputFile();

		assert inputFiles != null : "List of input artifacts may not be null!";

		for (FileArtifact inputFile : inputFiles) {
			assert (inputFile != null);
			assert (inputFile instanceof FileArtifact);
			if (inputFile.isDirectory() && !context.isRecursive()) {
				LOG.fatal("To merge directories, the argument '-r' "
						+ "has to be supplied. "
						+ "See '-help' for more information!");
				exit(context, -1);
			}
		}

		if (output != null && output.exists() && !output.isEmpty()) {
			System.err.println("Output directory is not empty!");
			System.err.println("Delete '" + output.getFullPath() + "'? [y/N]");
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					System.in));
			String response = reader.readLine();

			if (response.length() == 0
					|| response.toLowerCase().charAt(0) != 'y') {
				System.err.println("File not overwritten. Exiting.");
				exit(context, 1);
			} else {
				output.remove();
			}

		}

		if (context.isBugfixing()) {
			bugfixing(context);
		} else if (context.isDump()) {
			dump(context);
		} else {
			merge(context);
		}

		if (context.hasStats()) {
			StatsPrinter.print(context);
		}

		exit(context, 0);
	}

	/**
	 * Parses command line arguments and initializes program.
	 * 
	 * @param context
	 *            merge context
	 * @param args
	 *            command line arguments
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	private static void parseCommandLineArgs(final MergeContext context,
			final String[] args) throws IOException {
		assert (context != null);
		LOG.debug("parsing command line arguments: " + Arrays.toString(args));

		Options options = new Options();
		options.addOption("benchmark", false, "benchmark with " 
				+ context.getBenchmarkRuns() + " runs per file");
		options.addOption("debug", true, "set debug level");
		options.addOption("f", false, "force overwriting of output files");
		options.addOption("help", false, "print this message");
		options.addOption("keepgoing", false, 
				"Keep running after exceptions.");	
		options.addOption("mode", true,
				"set merge mode (textual, structured, combined, dumptree"
						+ ", dumpgraph, dumpfile, bugfixing)");
		options.addOption("output", true, "output directory/file");	
		options.addOption("r", false, "merge directories recursively");
		options.addOption("showconfig", false,
				"print configuration information");
		options.addOption("stats", false,
				"collects statistical data of the merge");
		options.addOption("stdout", false, "prints merge result to stdout");
		options.addOption("version", false,
				"print the version information and exit");

		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("help")) {
				help(context, options, 0);
			}

			if (cmd.hasOption("info")) {
				info(context, options, 0);
			}

			if (cmd.hasOption("version")) {
				version(context, true);
			}

			if (cmd.hasOption("debug")) {
				setLogLevel(cmd.getOptionValue("debug"));
			}

			if (cmd.hasOption("mode")) {
				try {
					if (cmd.getOptionValue("mode").equals("list")) {
						printStrategies(context, true);
					} else if (cmd.getOptionValue("mode").equals("bugfixing")) {
						context.setMergeStrategy(MergeStrategy
								.parse("structured"));
						context.setBugfixing();
					} else if (cmd.getOptionValue("mode").equals("dumptree")) {
						// User only wants to display the ASTs
						context.setMergeStrategy(MergeStrategy
								.parse("structured"));
						context.setDump(true);
						context.setGuiDump(false);
					} else if (cmd.getOptionValue("mode").equals("dumpgraph")) {
						// User only wants to display the ASTs
						context.setMergeStrategy(MergeStrategy
								.parse("structured"));
						context.setDump(true);
						context.setGuiDump(true);
					} else if (cmd.getOptionValue("mode").equals("dumpfile")) {
						// User only wants to display the files
						context.setMergeStrategy(MergeStrategy
								.parse("linebased"));
						context.setDump(true);
					} else {
						// User wants to merge
						context.setMergeStrategy(MergeStrategy.parse(cmd
								.getOptionValue("mode")));
					}
				} catch (StrategyNotFoundException e) {
					LOG.fatal(e.getMessage());
					exit(context, -1);
				}

				if (context.getMergeStrategy() == null) {
					help(context, options, -1);
				}
			}

			if (cmd.hasOption("output")) {
				context.setOutputFile(new FileArtifact(new Revision("merge"),
						new File(cmd.getOptionValue("output")), false));
			}

			context.setSaveStats(cmd.hasOption("stats") 
					|| cmd.hasOption("benchmark"));
			context.setBenchmark(cmd.hasOption("benchmark"));
			context.setForceOverwriting(cmd.hasOption("f"));
			context.setRecursive(cmd.hasOption("r"));
			context.setQuiet(!cmd.hasOption("stdout"));
			context.setKeepGoing(cmd.hasOption("keepgoing"));

			if (cmd.hasOption("showconfig")) {
				showConfig(context, true);
			}

			int numInputFiles = cmd.getArgList().size();

			if (!context.isDump() && !context.isBugfixing()
					&& numInputFiles < MergeType.MINFILES
					|| numInputFiles > MergeType.MAXFILES) {
				// number of input files does not fit
				help(context, options, 0);
			}

			// prepare the list of input files
			ArtifactList<FileArtifact> inputArtifacts 
				= new ArtifactList<FileArtifact>();

			for (Object filename : cmd.getArgList()) {
				try {
					inputArtifacts.add(new FileArtifact(new File(
							(String) filename)));
				} catch (FileNotFoundException e) {
					System.err.println("Input file not found: "
							+ (String) filename);
				}
			}

			context.setInputFiles(inputArtifacts);
		} catch (ParseException e) {
			LOG.fatal("arguments could not be parsed: " 
					+ Arrays.toString(args));
			LOG.fatal("aborting program");
			e.printStackTrace();
			exit(context, -1);
		}
	}

	/**
	 * Print short information and exit.
	 * 
	 * @param context
	 *            merge context
	 * @param options
	 *            Available command line options
	 * @param exitcode
	 *            the code to return on termination
	 */
	private static void info(final MergeContext context, final Options options,
			final int exitcode) {
		version(context, false);
		System.out.println();
		System.out.println("Run the program with the argument '--help' in "
				+ "order to retrieve information on its usage!");
		exit(context, exitcode);
	}

	/**
	 * Print help on usage and exit.
	 * 
	 * @param context
	 *            merge context
	 * @param options
	 *            Available command line options
	 * @param exitcode
	 *            the code to return on termination
	 */
	private static void help(final MergeContext context, final Options options,
			final int exitcode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(TOOLNAME, options, true);
		exit(context, exitcode);
	}

	/**
	 * Print version information and exit.
	 * 
	 * @param context
	 *            merge context
	 * @param exit
	 *            program exists if true
	 */
	private static void version(final MergeContext context, 
			final boolean exit) {
		System.out.println(TOOLNAME + " VERSION " + VERSION);

		if (exit) {
			exit(context, 0);
		}
	}

	/**
	 * Set the logging level. Default is DEBUG.
	 * 
	 * @param loglevel
	 *            May be OFF, FATAL, ERROR, WARN, INFO, DEBUG or ALL
	 */
	private static void setLogLevel(final String loglevel) {
		Logger.getRootLogger().setLevel(Level.toLevel(loglevel));
	}

	/**
	 * Exit program with provided return code.
	 * 
	 * @param context
	 *            merge context
	 * @param exitcode
	 *            the code to return on termination
	 */
	private static void exit(final MergeContext context, final int exitcode) {
		long programStop = System.currentTimeMillis();
		LOG.debug("stopping program");
		LOG.debug("runtime: " + (programStop - context.getProgramStart())
				+ " ms");
		LOG.debug("exit code: " + exitcode);
		System.exit(exitcode);
	}

	/**
	 * Prints configuration information.
	 * 
	 * @param context
	 *            merge context
	 * @param exit
	 *            whether to exit after printing the config
	 */
	private static void showConfig(final MergeContext context,
			final boolean exit) {
		assert (context != null);
		System.out.println("Merge strategy: " + context.getMergeStrategy());
		System.out.println();

		if (exit) {
			exit(context, 0);
		}
	}

	/**
	 * Prints the available strategies.
	 * 
	 * @param context
	 *            merge context
	 * @param exit
	 *            whether to exit after printing the strategies
	 */
	private static void printStrategies(final MergeContext context,
			final boolean exit) {
		System.out.println("Available merge strategies:");

		for (String s : MergeStrategy.listStrategies()) {
			System.out.println("\t- " + s);
		}

		if (exit) {
			exit(context, 0);
		}
	}

	/**
	 * Merges the input files.
	 * 
	 * @param context
	 *            merge context
	 * @throws InterruptedException
	 *             If a thread is interrupted
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public static void merge(final MergeContext context) throws IOException,
			InterruptedException {
		assert (context != null);
		Operation<FileArtifact> merge = new MergeOperation<FileArtifact>(
				context.getInputFiles(), context.getOutputFile());
		merge.apply(context);
	}

	/**
	 * @param context
	 *            merge context
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	@SuppressWarnings("unchecked")
	public static void dump(final MergeContext context) throws IOException {
		for (FileArtifact artifact : context.getInputFiles()) {
			MergeStrategy<FileArtifact> strategy 
				= (MergeStrategy<FileArtifact>) context.getMergeStrategy();
			strategy.dump(artifact, context.isGuiDump());
		}
	}

	/**
	 * @param context
	 *            merge context
	 * 
	 */
        private static void bugfixing(final MergeContext context) {
		context.setQuiet(false);
		setLogLevel("trace");
		
		for (FileArtifact artifact : context.getInputFiles()) {
		    ASTNodeArtifact ast = new ASTNodeArtifact(artifact);
		    System.out.println(ast.getASTNode().dumpTree());
		    System.out.println(ast.getASTNode());
		    System.out.println(ast.prettyPrint());
		}
	}

}
