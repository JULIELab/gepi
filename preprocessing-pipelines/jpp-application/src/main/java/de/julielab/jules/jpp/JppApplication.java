package de.julielab.jules.jpp;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JppApplication {

	private static final Logger log = LoggerFactory
			.getLogger(JppApplication.class);

	private enum Job {
		CLEAN_INSERT, UPDATE, PROCESSING_SCHEMA, RESET_SUBSET_TABLES
	}

	private Job job;
	private String medlineFile;

	public static void main(String[] args) {
		JppApplication app = new JppApplication();
		app.run(args);
	}

	private void run(String[] args) {
		parseArguments(args);

		try {
			switch (job) {
			case CLEAN_INSERT:
				Inserter inserter = new Inserter(medlineFile);
				inserter.process();
				break;
			case UPDATE:
				Updater updater = new Updater(medlineFile);
				updater.process();
				break;
			case PROCESSING_SCHEMA:
				ProcessingSubsetTableCreator subsetCreator = new ProcessingSubsetTableCreator();
				subsetCreator.process();
				break;
			case RESET_SUBSET_TABLES:
				SubsetTableResetter resetter = new SubsetTableResetter();
				resetter.process();
				break;
			default:
				throw new IllegalStateException("Unhandeled job: " + job);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void parseArguments(String[] args) {
		Options options = new Options();

		OptionGroup modes = new OptionGroup();
		Option option = buildOption(
				"i",
				"insert",
				"Insert MEDLINE documents into the database. The database must not contain any documents yet. For updates, see -u.",
				"MEDLINE file or directory of files");
		modes.addOption(option);
		option = buildOption(
				"u",
				"update",
				"Apply a MEDLINE update to an existing set of MEDLINE documents.",
				"MEDLINE update file or directory of update files.");
		modes.addOption(option);
		option = buildOption(
				"cps",
				"createprocessingschema",
				"Scans through the different sub-pipelines, tries to identify readers and consumers and creates the tables those components read from or write to. This can also be done when a new pipeline is added since already existing tables are left untouched.");
		modes.addOption(option);
		option = buildOption(
				"re",
				"reset",
				"Scans through the different sub-pipelines, tries to identify readers and consumers and resets the tables those components read from or write to for a complete update processing to be done.");
		modes.addOption(option);
		modes.setRequired(true);
		options.addOptionGroup(modes);

		option = buildOption(
				"d",
				"jppbasedir",
				"The base directory from which to find all jpp-subpipelines. Required for automatic creation of database tables needed by the subpipeline readers and consumers. Defaults to '../'.",
				"JPP base directory");
		options.addOption(option);

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			printCLIHelpAndExit(options);
		}

		job = null;
		if (cmd.hasOption("i")) {
			job = Job.CLEAN_INSERT;
			medlineFile = adjustPath(cmd.getOptionValue("i"));
		} else if (cmd.hasOption("u")) {
			job = Job.UPDATE;
			medlineFile = adjustPath(cmd.getOptionValue("u"));
		} else if (cmd.hasOption("cps")) {
			job = Job.PROCESSING_SCHEMA;
		} else if (cmd.hasOption("re")) {
			job = Job.RESET_SUBSET_TABLES;
		}
		
	}

	/**
	 * Adjusts the given path in a manner that the working directory can be the
	 * parent directory of the project directory. This is done in order that
	 * path parameters can be given intuitively without accounting for the fact
	 * that the JPP start script internally enters the jpp-application
	 * directory.
	 * 
	 * @param path
	 * @return
	 */
	private String adjustPath(String path) {
		String workingDirectory = new File(".").getAbsoluteFile()
				.getParentFile().getName();
		if (path.startsWith(workingDirectory))
			path = path.replaceFirst(workingDirectory + File.separator, "");
		return path;
	}

	private static void printCLIHelpAndExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(160);
		formatter.printHelp(JppApplication.class.getName(), options);
		System.exit(1);
	}

	private static Option buildOption(String shortName, String longName,
			String description, String... arguments) {
		OptionBuilder.withLongOpt(longName);
		OptionBuilder.withDescription(description);
		OptionBuilder.hasArgs(arguments.length);
		for (String argument : arguments)
			OptionBuilder.withArgName(argument);
		return OptionBuilder.create(shortName);
	}
}
