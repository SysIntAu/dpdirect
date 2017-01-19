package org.dpdirect.dpmgmt;

/**
 * Copyright 2016 Tim Goodwill
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

import org.dpdirect.schema.SchemaLoader;
import org.dpdirect.utils.Credentials;
import org.dpdirect.utils.DPDirectProperties;
import org.dpdirect.utils.FileUtils;

/**
 * Class for the management of IBM DataPower device via the XML management
 * interface.
 * 
 * Command line tool for IBM DataPower management.
 * 
 * Generates valid SOMA and AMP XML sets, and then posts to the target device in
 * order. SOMA and AMP Schema files are embedded in the jar file, but may be
 * over-ridden with new paths. SOMA and AMP operations should be 'stacked' to
 * minimise the schema loading and processing time, a single DPDirect 'session'
 * will work with a single instance of SchemaLoader and ResponseParser for
 * several operations.
 * 
 * Global options may include : port, username, userPassword, domain (default),
 * failOnError, rollbackOnError, verbose, SOMAschema, AMPschema.
 * 
 * Each stacked SOMA or AMP operation is created by setting an operation name
 * that corresponds to a valid SOMA or AMP operation. Operation names may be
 * checked and attributes identified by typing 'DPDirect find <operationName>'
 * from the cmd line. Eg. 'DPDirect find do-export'
 * 
 * See the method text for cmdLineHelp() ('DPDirect help') and antHelp()
 * ('DPDirect antHelp') for usage details.
 * 
 * Example Command Line usage:
 * 
 * <pre>
 * <code>
 * DPDirect DEV userName=EFGRTT userName=droWssaP operation=get-status class=ActiveUsers operation=RestartDomainRequest domain=SYSTEST
 * </code>
 * </pre>
 * 
 * @author Tim Goodwill
 */
public class DPDirect extends DPDirectBase implements DPDirectInterface {

	/**
	 * The command line prompt text.
	 */
	protected static final String CMD_PROMPT_TXT = "\n"
			+ DPDirect.class.getSimpleName() + "> ";

	/** Command-Line mode. Default 'false'. */
	protected boolean consoleMode = false;

	/** OutputType. Default 'PARSED'. */
	protected String outputType = "PARSED";
	
	/**
	 * Cache of the "console-usage.txt" help file content.
	 */
	protected static String consoleUsageText = null;

	/**
	 * Cache of the "cmd-usage.txt" help file content.
	 */
	protected static String cmdUsageText = null;

	/**
	 * Main class for command line invocation
	 * 
	 * @param args
	 *            [] String[] : command-line parameters. May include, in
	 *            order... 1. *.properties file name 2. global options 3. named
	 *            SOMA/AMP operation followed by operation options 4. any number
	 *            of repeats of 3.
	 * 
	 *            Example Command Line usage: DPDirect DEV userName=EFGRTT
	 *            userName=droWssaP operation=get-status class=ActiveUsers
	 *            operation=RestartDomainRequest domain=SYSTEST
	 */
	public static void main(String... args) {
		DPDirect dpSession = new DPDirect();
		dpSession.failOnError = false;
		Operation operation = null;

		String opName = null;
		String opValue = null;

		if (args.length < 1) {
			consoleMode(dpSession);
			return;
		}

		// 'special' options - help, antHelp or find
		String arg1 = args[0];
		if (arg1.startsWith("-")) {
			arg1 = arg1.substring(1, arg1.length());
		}
		if (arg1.equalsIgnoreCase("help") || arg1.equalsIgnoreCase("h")) {
			if (args.length > 1 && Constants.USAGE_HELP_ANT.equals(args[1])) {
				antHelp();
			} else if (args.length > 1
					&& Constants.USAGE_HELP_CONSOLE.equals(args[1])) {
				consoleHelp();
			} else {
				cmdLineHelp();
			}
			System.exit(0);
		}
		if (arg1.equalsIgnoreCase("find") || arg1.equalsIgnoreCase("f")) {
			dpSession.sampleOperation(args[1]);
			System.exit(0);
		}
		if (arg1.contains("find=")) {
			dpSession.sampleOperation(arg1.replace("find=", ""));
			System.exit(0);
		}

		for (int i = 0; i < args.length; i++) {
			String option = (String) args[i];
			if (option.startsWith("-")) {
				option = option.substring(1, option.length());
			}
			if (option.indexOf("=") > 0) {
				opName = option.substring(0, option.indexOf("="));
				opValue = option.substring(option.indexOf("=") + 1,
						option.length());
			} else {
				opName = option;
			}

			// process properties file
			if (i == 0 && (!args[0].contains("="))
					|| opName.equalsIgnoreCase("properties")) {
				String propFileName = args[0];
				dpSession.processPropertiesFile(propFileName);

				// process consoleMode parameters
			} else {
				if (!opName.equalsIgnoreCase("operation")) {
					if (operation == null) {
						dpSession.setGlobalOption(opName, opValue);
					} else if (operation != null) {
						if (opName.endsWith("file") || opName.endsWith("File")) {
							File f = new File(opName);
							if (f.exists()) {
								try {
									operation.addOption(opName, f);
								} catch (IOException ex) {
									if (!dpSession.failOnError) {
										log.error(ex.getMessage());
									} else {
										log.error(ex.getMessage(), ex);
										System.exit(1);
									}
								}
							}
						} else {
							operation.addOption(opName, opValue);
						}
					}
				} else {
					operation = dpSession.createOperation();
					operation.setName(opValue);
				}
			}
		}

//		// prompt for user credentials if not supplied.
//		if (null == dpSession.getCredentials()) {
//			Credentials credentials = FileUtils.promptForLogonCredentials();
//			dpSession.setCredentials(credentials);
//		}

		if (operation != null) {
			dpSession.execute();
		} else {
			if (null == dpSession.getHostName()) {
				log.warn("Hostname not set. Set with \"hostName=<name>\"");
			}
			dpSession.setSchema();
			consoleMode(dpSession);
		}
	}

	/**
	 * Console Mode - process one operation at a time.
	 * 
	 * @param dpSession
	 *            DPDirect : instance of the class.
	 * 
	 *            First word corresponds to a single operation name. Operation
	 *            options follow (representing attribute and element values) in
	 *            <name>=<value> format. Keywords 'exit', 'end', 'quit' and 'q'
	 *            will exit console mode.
	 */
	public static void consoleMode(DPDirect dpSession) {
		
		// prompt for user credentials if not supplied.
		if (null == dpSession.getCredentials()) {
			Credentials credentials = FileUtils.promptForLogonCredentials();
			dpSession.setCredentials(credentials);
		}
		
		dpSession.consoleMode = true;
		String opName = null;
		String opValue = null;
		Operation operation = null;
		String input = "";

		System.out.println("Console mode. Type 'quit' or 'q' to exit.");

		Scanner in = new Scanner(System.in);

		nextCmd: while (in != null) {
			dpSession.resetOperationChain();
			operation = null;
			// Echo command prompt text to the console.
			System.out.print(CMD_PROMPT_TXT);
			input = in.nextLine();
			if (input.equals("")) {
				continue;
			}
			if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("end")
					|| input.equalsIgnoreCase("quit")
					|| input.equalsIgnoreCase("q")) {
				// Quit
				System.exit(0);
			} else {
				String newArgs[] = input.split("\\s+");
				if (newArgs.length > 0) {
					int firstIndex = 0;
					String operationName = newArgs[firstIndex];
					if (operationName.equalsIgnoreCase("find")
							&& newArgs[1] != null) {
						dpSession.sampleOperation(newArgs[1]);
					} else if (operationName.equalsIgnoreCase("help")) {
						if (newArgs.length > 1
								&& Constants.USAGE_HELP_ANT.equals(newArgs[1])) {
							antHelp();
						} else if (newArgs.length > 1
								&& Constants.USAGE_HELP_CMDLINE
										.equals(newArgs[1])) {
							cmdLineHelp();
						} else {
							consoleHelp();
						}
					} else if (operationName.indexOf("=") < 0) {
						operation = dpSession.createOperation();
						operation.setName(operationName);
						firstIndex = 1;
					}
					for (int i = firstIndex; i < newArgs.length; i++) {
						String option = (String) newArgs[i];
						if (option.startsWith("-")) {
							option = option.substring(1, option.length());
						}
						if (option.indexOf("=") > 0) {
							opName = option.substring(0, option.indexOf("="));
							opValue = option.substring(option.indexOf("=") + 1,
									option.length());
						} else {
							opName = option;
						}
						if (operation != null) {
							if (opName.endsWith("file")
									|| opName.endsWith("File")) {
								try {
									File f = new File(opName);
									if (f.exists()) {
										operation.addOption(opName, f);
									} else {
										operation.addOption(opName, opValue);
									}
								} catch (Exception ex) {
									log.error(ex.getMessage());
									continue nextCmd;
								}
							} else {
								operation.addOption(opName, opValue);
							}
						} else {
							if (opName.equalsIgnoreCase("properties")) {
								dpSession.processPropertiesFile(opValue);
							} else {
								dpSession.setGlobalOption(opName, opValue);
							}
						}
					}
				}
			}
			if (operation != null) {
				if (operation.isPolling()) {
					DPCustomOp customOp = operation.getCustomOperation();
					// poll the device.
					System.out
							.println("Type 'enter' to return to cmd prompt.\n");

					if (Constants.TAIL_LOG_CUSTOM_OP_NAME.equals(customOp.getName())
							&& null == operation
									.getOptionValue(Constants.NAME_OPT_NAME)) {
						operation.addOption(Constants.NAME_OPT_NAME,
								Constants.LOGTEMP_DIR_NAME + ":/"
										+ Constants.DEFAULT_LOG_FILE_NAME);
					}

					DataInputStream dis = new DataInputStream(System.in);
					try {
						while (0 == dis.available()) {
							String responseXML = dpSession
									.generateAndPost(operation);
							operation.setResponse(responseXML);
							boolean success = customOp.processTail(operation);
							if (!success) {
								break;
							}
							long stopNanos = System.nanoTime()
									+ TimeUnit.MILLISECONDS.toNanos(customOp
											.getPollIntMillis());
							while (System.nanoTime() < stopNanos) {
								if (0 != dis.available()) {
									break;
								}
							}
						}
					} catch (IOException ex) {
						log.error(ex.getMessage());
						continue nextCmd;
					} finally {
						if (Constants.TAIL_COUNT_CUSTOM_OP_NAME
								.equals(customOp.getName())) {
							boolean removedMonitor = customOp.removeMonitor();
							if (removedMonitor) {
								log.info("Monitor removed successfully.");
							} else {
								log.info("Failed to remove Monitor.");
							}
						}
					}
				} else if (null != operation.getCustomOperation()) {
					dpSession.generateOperationXML();
					dpSession.postOperationXML();
				} else {
					String responseXML = dpSession.generateAndPost(operation);
					if (null != responseXML) {
						operation.response = responseXML;
						dpSession.processResponse(operation);
					}
				}
			}
		}
	}
	
	/**
	 * Print command line help to the console.
	 */
	public static void help() {
		cmdLineHelp();
	}

	/**
	 * Print command line help to the console.
	 */
	public static void cmdLineHelp() {
		if (null == cmdUsageText) {
			System.out.println("Failed to locate cmd line usage text.");
		} else {
			System.out.print(cmdUsageText);
			System.out.println();
		}
	}

	/**
	 * Print console help to System.out.
	 */
	public static void consoleHelp() {
		if (null == consoleUsageText) {
			System.out.println("Failed to locate console usage text.");
		} else {
			System.out.print(consoleUsageText);
			System.out.println();
		}
	}

	/**
	 * Print ant task help to System.out.
	 */
	public static void antHelp() {
		if (null == antUsageText) {
			System.out.println("Failed to locate ant usage text.");
		} else {
			System.out.print(antUsageText);
			System.out.println();
		}
	}
	
	/**
	 * @param tailLogLines
	 *            the tailLogLines to set
	 */
	public static void setDefaultTailLines(int tailLogLines) {
		DPCustomOp.setDefaultTailLines(tailLogLines);
	}

	/**
	 * Constructs a new <code>DPDirect</code> class.
	 */
	public DPDirect() {
		super();
		setLogOutput(false);
		// Load properties
		try {
			this.props = new DPDirectProperties();
			try {
				setDefaultTailLines(Integer.parseInt(props
						.getProperty(DPDirectProperties.TAIL_LOG_LINES_KEY)));
			} catch (Exception e) {
				// do nothing
			}
		} catch (IOException ex) {
			if (!failOnError && !log.isDebugEnabled()) {
				log.error(ex.getMessage());
			} else {
				log.error(ex.getMessage(), ex);
			}
		}
		// Cache the command line and console text file content.
		InputStream inputStream = DPDirect.class
				.getResourceAsStream(Constants.CONSOLE_USAGE_TEXT_FILE_PATH);
		try {
			byte[] fileBytes = FileUtils.readInputStreamBytes(inputStream);
			consoleUsageText = new String(fileBytes);
		} catch (IOException ex) {
			if (!failOnError && !log.isDebugEnabled()) {
				log.error(ex.getMessage());
			} else {
				log.error(ex.getMessage(), ex);
			}
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
				// Ignore.
			}
		}
		inputStream = DPDirect.class
				.getResourceAsStream(Constants.CMD_USAGE_TEXT_FILE_PATH);
		try {
			byte[] fileBytes = FileUtils.readInputStreamBytes(inputStream);
			cmdUsageText = new String(fileBytes);
		} catch (IOException ex) {
			log.error(ex.getMessage(), ex);
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
				// Ignore.
			}
		}
	}

	/**
	 * Constructs a new <code>DPDirect</code> class.
	 * 
	 * @param schemaDirectory
	 *            the directory in which to find the SOMA and AMP schema.
	 */
	public DPDirect(String schemaDirectory) {
		this();
		setLogOutput(false);
		try {
			schemaLoaderList.add(new SchemaLoader(schemaDirectory + "/"
					+ Constants.SOMA_MGMT_SCHEMA_NAME));
			log.debug("SOMAInstance schemaURI : "
					+ schemaLoaderList.get(schemaLoaderList.size() - 1)
							.getSchemaURI());
		} catch (Exception ex) {
			if (!failOnError && !log.isDebugEnabled()) {
				log.error(ex.getMessage());
			} else {
				log.error(ex.getMessage(), ex);
			}
		}
	}
	
	/**
	 * Print out sample XML for nodes that match the given regex. invoked via
	 * the cmdline 'find' option
	 * 
	 * @param regex
	 *            a regular expression used to match node names
	 * @return return ArrayList of element names or empty ArrayList if none
	 *         found
	 */
	public void sampleOperation(String regex) {
		setSchema();
		List<String> sampleList = new ArrayList<String>();
		try {
			for (SchemaLoader loader : schemaLoaderList) {
				sampleList.addAll(loader.findMatch(regex, true));
			}
			for (String sampleXML : sampleList) {
				if (!sampleXML.contains(Constants.SOMA_RESPONSE_IDENTIFIER)
						&& !sampleXML
								.contains(Constants.AMP_RESPONSE_IDENTIFIER)) {
					System.out.println("# Sample XML:");
					System.out.println(sampleXML);
				}
			}
		} catch (PatternSyntaxException ex) {
			log.error("Bad regex : " + ex.getMessage());
		} catch (Exception ex) {
			if (!failOnError && !log.isDebugEnabled()) {
				log.error(ex.getMessage());
			} else {
				log.error(ex.getMessage(), ex);
			}
		}
	}

}
