package de.julielab.jules.jpp;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.NotImplementedException;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.util.InvalidXMLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.consumer.CasToXmiDBConsumer;
import de.julielab.jules.cpe.DBCPERunner;

public class JPPDBCPERunner extends DBCPERunner {

	private final static Logger log = LoggerFactory
			.getLogger(JPPDBCPERunner.class);

	boolean updateMode = false;

	public JPPDBCPERunner() {
		super();
		options.addOption("u", false, "Update mode (optional)");
	}
	
	public static void main(String[] args) {
		JPPDBCPERunner runner = new JPPDBCPERunner();
		runner.process(args);
	}
	
	@Override
	public void parseArguments(String[] args) {
		super.parseArguments(args);
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			log.error("Can't parse arguments:", e);
			return;
		}
		if (cmd.hasOption("u"))
			updateMode = true;
	}

	@Override
	public void createCPEDescription() throws InvalidXMLException, IOException,
			CpeDescriptorException {
		super.createCPEDescription();
		Utilities.setCasProcessorParameterValue("xmi.*consumer",
				CasToXmiDBConsumer.PARAM_UPDATE_MODE, updateMode,
				cpeDescription);
	}

//	@Override
//	public void createCPE() throws InvalidXMLException, IOException,
//			ResourceInitializationException, CpeDescriptorException {
//		super.createCPE();
//		// The super class adds the default statusCallbackListener. We remove it
//		// and replace it by our own extended version.
//		cpe.removeStatusCallbackListener(statusCallbackListener);
//		statusCallbackListener = new JPPDBStatusCallbackListener(cpe, dbc,
//				subset, batchSize);
//		cpe.addStatusCallbackListener(statusCallbackListener);
//	}

}
