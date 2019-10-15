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
 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dpdirect.dpmgmt.DPDirectBase.Operation.Option;
import org.dpdirect.schema.DocumentHelper;
import org.dpdirect.schema.SchemaLoader;
import org.dpdirect.utils.Credentials;
import org.dpdirect.utils.DPDirectProperties;
import org.dpdirect.utils.FileUtils;
import org.dpdirect.utils.PostXML;

/**
 * Base Class for the management of IBM DataPower device via the XML management
 * interface.
 * 
 * For Ant task implementation see 'DPDirectTask' Class, 
 * for command and console line tool see 'DPDirect' Class.
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
 * 
 * Example Ant usage:
 * 
 * <pre>
 * <code>
 * <target name="testDeploy">
 *     <taskdef name="dpDeploy" classname="org.dpdirect.dpmgmt.DPDirectTask" classpath="DPDirect.jar"/>
 *     <dpDeploy domain="SCRATCH" verbose="true" userName="EFGRTT" userPassword="droWssaP">
 *        <operation name="SaveConfig" />
 *        <operation name="do-import">
 *           <option name="do-import" srcFile="C:/temp/SCRATCH.zip"/>
 *           <option name="overwrite-files" value="true"/>
 *        </operation>
 *     </dpDeploy>
 *  </target>
 *  </code>
 * </pre>
 * 
 * @author Tim Goodwill
 */
public class DPDirectBase implements DPDirectInterface {
	
	/** 
	 * Default firmware level - determines SOMA and AMP version. 
	 */
	public static int DEFAULT_FIRMWARE_LEVEL = 5;
	
	/** 
	 * Default waitTime - determines wait time when polling for a result. 
	 */
	public static int DEFAULT_WAIT_TIME_SECONDS = 30;
	
	/**
	 * A default value for the log poll interval as a value in milliseconds.
	 */
	public static int DEFAULT_POLL_INT_MILLIS = 2000;

	/**
	 * Class logger.
	 */
	protected final static Logger log = Logger.getLogger(DPDirectBase.class);

	/**
	 * Cache of project properties.
	 */
	protected DPDirectProperties props = null;

	/**
	 * The system dependent path of the NETRC file, optionally used for
	 * credential lookup.
	 */
	public String netrcFilePath = null;

	/** Nominated firmware level - determines SOMA and AMP version. */
	protected int firmwareLevel = DEFAULT_FIRMWARE_LEVEL;
	protected String userFirmwareLevel = "default";

	/**
	 * Date formatter object configured with 'yyyyMMddhhmmss' format. Caller
	 * should synchronize on this object prior to use.
	 */
	protected static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(
			"yyyyMMddhhmmss");

	/** List of operations to build and post in order. */
	protected List<Operation> operationChain = new ArrayList<Operation>();

	/** List of loaded SchemaLoader schemas */
	protected List<SchemaLoader> schemaLoaderList = new ArrayList<SchemaLoader>();

	/** OutputType. Default 'PARSED'. */
	protected String outputType = "PARSED";

	/** Target DataPower device hostname. */
	protected String hostName = null;

	/** Target DataPower port number. Default '5550'. */
	protected String port = "5550";

	/** Target DataPower username and password */
	protected Credentials credentials = null;

	/**
	 * Optional Target DataPower domain. Constitutes default for chained
	 * operations.
	 */
	protected String domain = null;

	/** Checkpoint saved, and rolled back in case of deployment errors. */
	protected String checkPointName = null;

	/** Operations immediately cease if an error is encountered. Default 'true'. */
	protected boolean failOnError = true;
	
	/** Output is logged. Default 'true'. */
	protected boolean logOutput = true;
	
	/**
	 * Cache of the "ant-usage.txt" help file content.
	 */
	protected static String antUsageText = null;

	/**
	 * Print ant help to the console.
	 */
	public static void help() {
		antHelp();
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
	 * @param pollIntMillis
	 *            the pollIntMillis to set
	 */
	public static void setDefaultPollIntMillis(int pollIntMillis) {
		DEFAULT_POLL_INT_MILLIS = pollIntMillis;
	}
	
	/**
	 * @returns default pollIntMillis
	 */
	public static int getDefaultPollIntMillis() {
		return DEFAULT_POLL_INT_MILLIS;
	}

	/**
	 * Constructs a new <code>DPDirect</code> class.
	 */
	public DPDirectBase() {
		log.debug("Constructing new DPDirect class intance");
		// Load properties
		try {
			this.props = new DPDirectProperties();
			try {
				setNetrcFilePath(props
						.getProperty(DPDirectProperties.NETRC_FILE_PATH_KEY));
				setFirmware(props
						.getProperty(DPDirectProperties.FIRMWARE_LEVEL_KEY));
			} catch (Exception e) {
				this.firmwareLevel = DEFAULT_FIRMWARE_LEVEL;
			}
		} catch (IOException ex) {
			if (!failOnError && !log.isDebugEnabled()) {
				log.error(ex.getMessage());
			} else {
				log.error(ex.getMessage(), ex);
			}
		}
		// Cache the ant usage text file content.
		InputStream inputStream = DPDirectBase.class
				.getResourceAsStream(Constants.ANT_USAGE_TEXT_FILE_PATH);
		try {
			byte[] fileBytes = FileUtils.readInputStreamBytes(inputStream);
			antUsageText = new String(fileBytes);
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
	public DPDirectBase(String schemaDirectory) {
		this();
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
	 * @return this instance
	 */
	protected DPDirectBase getDPDInstance() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#execute()
	 */
	@Override
	public void execute() {
		// prompt for user credentials if not supplied.
		if (null == this.getCredentials()) {
			Credentials credentials = FileUtils.promptForLogonCredentials();
			setCredentials(credentials);
		}
		setSchema();
		this.generateOperationXML();
		this.postOperationXML();
	}
	
	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#getOperationChain()
	 */
	@Override
	public List<Operation> getOperationChain() {
		return this.operationChain;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#addToOperationChain(org.dpdirect.dpmgmt.DPDirect.Operation)
	 */
	public void addToOperationChain(Operation operation) {
		((List<Operation>)getOperationChain()).add(operation);
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#addToOperationChain(org.dpdirect.dpmgmt.DPDirect.Operation)
	 */
	public void addToOperationChain(int i, Operation operation) {
		((List<Operation>)getOperationChain()).add(i, operation);
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#resetOperationChain()
	 */
	@Override
	public void resetOperationChain() {
		operationChain.clear();
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#resetSchemas()
	 */
	@Override
	public void resetSchemas() {
		schemaLoaderList.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setSchema()
	 */
	@Override
	public void setSchema() {
		try {
			if (schemaLoaderList.isEmpty()) {
				if (firmwareLevel == 0) {
					schemaLoaderList.add(new SchemaLoader(this.getClass()
							.getResource(Constants.SOMA_MGMT_DEFAULT_SCHEMA_PATH)
							.toExternalForm()));
					log.debug("SOMAInstance schemaURI : "
							+ schemaLoaderList.get(schemaLoaderList.size() - 1)
									.getSchemaURI());

					schemaLoaderList.add(new SchemaLoader(this.getClass()
							.getResource(Constants.AMP_MGMT_DEFAULT_SCHEMA_PATH)
							.toExternalForm()));
					log.debug("AMPInstance schemaURI : "
							+ schemaLoaderList.get(schemaLoaderList.size() - 1)
									.getSchemaURI());
				} else if (firmwareLevel >= 3) {
				    schemaLoaderList.add(new SchemaLoader(this.getClass()
							.getResource(Constants.MGMT_SCHEMAS_DIR + '/' + userFirmwareLevel 
							+ '/' + Constants.SOMA_MGMT_SCHEMA_NAME).toExternalForm()));
					log.debug("SOMAInstance schemaURI : "
							+ schemaLoaderList.get(schemaLoaderList.size() - 1)
									.getSchemaURI());

					schemaLoaderList.add(new SchemaLoader(this.getClass()
							.getResource(Constants.MGMT_SCHEMAS_DIR + '/' + userFirmwareLevel + '/' 
							+ Constants.AMP_MGMT_DEFAULT_SCHEMA_NAME).toExternalForm()));
					log.debug("AMPInstance schemaURI : "
							+ schemaLoaderList.get(schemaLoaderList.size() - 1)
									.getSchemaURI());
				} else {
										schemaLoaderList.add(new SchemaLoader(this.getClass()
							.getResource(Constants.SOMA_MGMT_DEFAULT_SCHEMA_PATH)
							.toExternalForm()));
					log.debug("SOMAInstance schemaURI : "
							+ schemaLoaderList.get(schemaLoaderList.size() - 1)
									.getSchemaURI());

					schemaLoaderList.add(new SchemaLoader(this.getClass()
							.getResource(Constants.AMP_MGMT_DEFAULT_SCHEMA_PATH)
							.toExternalForm()));
					log.debug("AMPInstance schemaURI : "
							+ schemaLoaderList.get(schemaLoaderList.size() - 1)
									.getSchemaURI());
				}
			}
		} catch (Exception ex) {
			if (!failOnError && !log.isDebugEnabled()) {
				log.error(ex.getMessage());
			} else {
				log.error(ex.getMessage(), ex);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setSchema(java.lang.String)
	 */
	@Override
	public void setSchema(String schemaPath) {

		try {
			if (Constants.SOMA_MGMT_2004_SHORT.equalsIgnoreCase(schemaPath)) {
				schemaLoaderList.add(0, new SchemaLoader(this.getClass()
						.getResource(Constants.SOMA_MGMT_DEFAULT_SCHEMA_PATH)
						.toExternalForm()));
				log.debug("Added schemaURI : "
						+ schemaLoaderList.get(schemaLoaderList.size() - 1)
								.getSchemaURI());
			} else {
				schemaLoaderList.add(0, new SchemaLoader(this.getClass()
						.getResource(Constants.AMP_MGMT_DEFAULT_SCHEMA_PATH)
						.toExternalForm()));
				log.debug("Added schemaURI : "
						+ schemaLoaderList.get(schemaLoaderList.size() - 1)
								.getSchemaURI());
				schemaLoaderList.add(0, new SchemaLoader(this.getClass()
						.getResource(Constants.SOMA_MGMT_DEFAULT_SCHEMA_PATH)
						.toExternalForm()));
				log.debug("Added schemaURI : "
						+ schemaLoaderList.get(schemaLoaderList.size() - 1)
								.getSchemaURI() + "\n");
//			} else {
//				schemaLoaderList.add(0, new SchemaLoader(schemaPath));
//				log.debug("Added schemaURI : "
//						+ schemaLoaderList.get(schemaLoaderList.size() - 1)
//								.getSchemaURI());
			}
		} catch (Exception ex) {
			if (!failOnError && !log.isDebugEnabled()) {
				log.error(ex.getMessage());
			} else {
				log.error(ex.getMessage(), ex);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#processPropertiesFile(java.lang.String)
	 */
	@Override
	public void processPropertiesFile(String propFileName) {
		final String PROP_SUFFIX = ".properties";
		String opName = null;
		String opValue = null;
		// remove .properties extension if it exists - Resource loader will add
		// the extension
		if ((propFileName.length() < PROP_SUFFIX.length())
				|| !propFileName.substring(
						propFileName.length() - PROP_SUFFIX.length(),
						propFileName.length()).equals(PROP_SUFFIX)) {
			propFileName = propFileName + PROP_SUFFIX;
		}
		try {
			File propFile = new File(propFileName);
			String propFilePath = propFile.getParent();
			if (propFilePath == null && !propFile.exists()) {
				String filePath = FileUtils.class.getProtectionDomain()
						.getCodeSource().getLocation().getPath();
				if (System.getProperty("os.name").startsWith("Windows")){
					filePath = filePath.substring(1, filePath.length());
				}
				File jarFile = new File(filePath);
				propFilePath = jarFile.getParent();
				log.debug(propFilePath + "/" + propFileName);
				propFile = new File(propFilePath + "/" + propFileName);
			}
			Properties props = FileUtils.loadProperties(propFile);
			for (Object key : props.keySet()) {
				opName = (String) key;
				opValue = props.getProperty(opName);
				setGlobalOption(opName, opValue);
			}
		} catch (Exception ex) {
			log.error("Error. Could not locate properties file '"
					+ propFileName + "'");
			help();
			System.exit(0);
		}
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setGlobalOption(java.lang.String, java.lang.String)
	 */
	@Override
	public void setGlobalOption(String name, String value) {
		if (Constants.HOST_NAME_OPT_NAME.equalsIgnoreCase(name)) {
			this.setHostName(value);
		} else if (Constants.PORT_OPT_NAME.equalsIgnoreCase(name)) {
			this.setPort(value);
		} else if (Constants.USER_NAME_OPT_NAME.equalsIgnoreCase(name)) {
			this.setUserName(value);
		} else if (Constants.USER_PASSWORD_OPT_NAME.equalsIgnoreCase(name)) {
			this.setUserPassword(value);
		} else if (Constants.DOMAIN_OPT_NAME.equalsIgnoreCase(name)) {
			this.setDomain(value);
		} else if (Constants.FAIL_ON_ERROR_OPT_NAME.equalsIgnoreCase(name)) {
			this.setFailOnError(Boolean.getBoolean(value));
		} else if (Constants.SCHEMA_OPT_NAME.equalsIgnoreCase(name)) {
			this.setSchema(value);
		} else if (Constants.OUTPUT_TYPE_OPT_NAME.equalsIgnoreCase(name)) {
			this.setOutputType(value);
		} else if (Constants.FIRMWARE_OPT_NAME.equalsIgnoreCase(name)) {
			this.setFirmware(value);
			if (!this.schemaLoaderList.isEmpty()) {
				resetSchemas();
				setSchema();
			}
		} else if (Constants.DEBUG_OPT_NAME.equalsIgnoreCase(name)
				&& Constants.TRUE_OPT_VALUE.equalsIgnoreCase(value)) {
			log.setLevel(org.apache.log4j.Level.DEBUG);
		} else if (Constants.DEBUG_OPT_NAME.equalsIgnoreCase(name)
				&& Constants.FALSE_OPT_VALUE.equalsIgnoreCase(value)) {
			log.setLevel(org.apache.log4j.Level.INFO);
		} else if (Constants.VERBOSE_OPT_NAME.equalsIgnoreCase(name)
				&& Constants.TRUE_OPT_VALUE.equalsIgnoreCase(value)) {
			log.setLevel(org.apache.log4j.Level.DEBUG);
		} else if (Constants.VERBOSE_OPT_NAME.equalsIgnoreCase(name)
				&& Constants.FALSE_OPT_VALUE.equalsIgnoreCase(value)) {
			log.setLevel(org.apache.log4j.Level.INFO);
		}

	}
	
	/**
	 * get the logger attached to this class.
	 * @return logger
	 */
	public Logger getLogger() {
		return log;
	}
	
	/**
	 * set the logOutput switch.
	 * @param should the output be logged.
	 */
	public void setLogOutput(boolean isLogged){
		this.logOutput = isLogged;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#getOutputType()
	 */
	@Override
	public String getOutputType() {
		return this.outputType;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setOutputType(java.lang.String)
	 */
	@Override
	public void setOutputType(String type) {
		this.outputType=type;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setHostName(java.lang.String)
	 */
	@Override
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setPort(java.lang.String)
	 */
	@Override
	public void setPort(String port) {
		this.port = port;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#getPort()
	 */
	@Override
	public String getPort() {
		return port;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setUserName(java.lang.String)
	 */
	@Override
	public void setUserName(String userName) {
		if (null == credentials) {
			this.setCredentials(new Credentials());
		}
		this.getCredentials().setUserName(userName);
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setUserPassword(java.lang.String)
	 */
	@Override
	public void setUserPassword(String password) {
		if (null == credentials) {
			this.setCredentials(new Credentials());
		}
		this.getCredentials().setPassword(password.toCharArray());
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#getCredentials()
	 */
	@Override
	public Credentials getCredentials() {
		// If credentials are null then they have not been provided by the
		// command line or
		// ant task and we default to Netrc file lookup.
		if (null == credentials) {
			if (null == getHostName()) {
				log.error("Failed to resolve credentials from Netrc config. No target 'hostName' value has been provided");
				return null;
			}
			try {
				log.debug("Resolving credentials from Netrc config.");
				credentials = getCredentialsFromNetrcConfig(getHostName());
				if (log.isDebugEnabled()) {
					log.debug("Resulting username from Netrc config: username="
							+ ((null == credentials) ? null : credentials
									.getUserName()));
				}
				if (null == credentials) {
					log.error("Failed to resolve credentials. Credential have not been provided for the target host either by command line or ant task or Netrc config file.");
				}
			} catch (Exception ex) {
				log.error(
						"Failed to resolve credentials from Netrc config. Error msg: "
								+ ex.getMessage(), ex);
			}
		}
		return credentials;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setCredentials(org.dpdirect.utils.Credentials)
	 */
	@Override
	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#getHostName()
	 */
	@Override
	public String getHostName() {
		return this.hostName;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setDomain(java.lang.String)
	 */
	@Override
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#getDomain()
	 */
	@Override
	public String getDomain() {
		return this.domain;
	}
	
	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#getDomain()
	 */
	public String getDefaultDomain() {
		return this.domain;
	}


	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setVerbose(java.lang.String)
	 */
	@Override
	public void setVerbose(String verboseOutput) {
		setDebug(verboseOutput);
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setDebug(java.lang.String)
	 */
	@Override
	public void setDebug(String debugOutput) {
		if (Constants.TRUE_OPT_VALUE.equalsIgnoreCase(debugOutput)) {
			log.setLevel(org.apache.log4j.Level.DEBUG);
		} else if (Constants.FALSE_OPT_VALUE.equalsIgnoreCase(debugOutput)) {
			log.setLevel(org.apache.log4j.Level.INFO);
		}
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setFailOnError(boolean)
	 */
	@Override
	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setRollbackOnError(boolean)
	 */
	@Override
	public void setRollbackOnError(boolean enableRollback) {
		if (enableRollback) {
			// create new operation, insert at the top of the operationChain.
			synchronized (DATE_FORMATTER) {
				checkPointName = "CP" + DATE_FORMATTER.format(new Date());
			}
			Operation operation = new Operation(
					Constants.SAVE_CHECKPOINT_OP_NAME);
			operation.addOption(Constants.CHK_NAME_OP_NAME, checkPointName);
			addToOperationChain(0, operation);
			failOnError = true;
		} else {
			checkPointName = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#removeCheckpoint()
	 */
	@Override
	public void removeCheckpoint() {
		Operation removeCheckpoint = new Operation(
				Constants.REMOVE_CHECKPOINT_OP_NAME);
		removeCheckpoint.addOption(Constants.CHK_NAME_OP_NAME, checkPointName);

		String xmlResponse = generateAndPost(removeCheckpoint);
		removeCheckpoint.setResponse(xmlResponse);
		String parsedText = parseResponseMsg(removeCheckpoint, false);
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#createOperation()
	 */
	@Override
	public Operation createOperation() {
		Operation operation = new Operation();
		addToOperationChain(operation);
		return operation;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#createOperation(java.lang.String)
	 */
	@Override
	public Operation createOperation(String operationName) {
		Operation operation = new Operation(operationName);
		addToOperationChain(operation);
		return operation;
	}
	
	public Operation newOperation() {
		return new Operation();
	}
	
	public Operation newOperation(String operationName) {
		return new Operation(operationName);
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#addOperation(java.lang.String)
	 */
	@Override
	public void addOperation(String operationName) {
		Operation operation = new Operation(operationName);
		addToOperationChain(operation);
	}

	/**
	 * Iterate through the operationChain to generate the SOMA and AMP XML. Will
	 * exit upon failure to generate valid XML.
	 */
	protected void generateOperationXML() {
		if (getOperationChain().isEmpty()) {
			DPDirectBase.antHelp();
		}
		for (Operation operation : getOperationChain()) {
			if (!operation.getMemSafe()) {
				generateXMLInstance(operation);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#generateXMLInstance(org.dpdirect.dpmgmt.DPDirect.Operation)
	 */
	@Override
	public String generateXMLInstance(Operation operation) {
		String xmlString = null;
		SchemaLoader workingInstance = null;
		String operationName = operation.getName();
		log.debug("GenerateXMLInstance - operation : " + operationName);
		List<Option> options = operation.getOptions();

		// Discern the target operation schema, and assign the DP device
		// endpoint.
		for (SchemaLoader loader : schemaLoaderList) {
			if (loader.nodeExists(operationName)) {
				workingInstance = loader;
				operation.defineEndPoint(loader);
			}
		}
		
		try {
			if (null == workingInstance) {
				if (failOnError) {
					throw new Exception(
							"No such operation available in the versions of SOMA and/or AMP schemas provided.");
				} else {
					logError(operation, "No such operation available in the loaded versions of SOMA and/or AMP schemas.");
					return null;
				}
			} else {

				workingInstance.newDocument();
				workingInstance.setTargetNode(operationName);
				workingInstance.setSoapEnv();
				
		        // unqualified get-status custom operation 
		        if (Constants.GET_STATUS_OP_NAME.equals(operationName)) {
		            if (null == operation.getOptionValue(Constants.CLASS_OPT_NAME)) {
		            	operation.addOption(Constants.FILTER_OUT_OPT_NAME, Constants.EXPECTED_STATUS_RESPONSE);
		            	workingInstance.setValue(Constants.CLASS_OPT_NAME, Constants.OBJECT_STATUS_OPT_VALUE);
		            }
		        }

				for (Option option : options) {
					// set operation options in the SchemaLoader model
					String optionName = option.getName();
					String optionValue = option.getValue();

					if (log.isDebugEnabled()) {
						String optionString = "null";
						if (null != optionValue) {
							optionString = optionValue;
						}
						if (optionString.length() > 500) {
							optionString = optionString.substring(0, 200)
									+ "... \n* truncated *";
						}
						log.debug("option : name=" + optionName + ", value="
								+ optionString);
					}
					
					if (Constants.DOMAIN_OPT_NAME.equals(optionName)) {
						operation.updateDomainName(optionValue);
						workingInstance.setValue(Constants.DOMAIN_OPT_NAME, optionValue);
						if (operation.isAMP){
							workingInstance.setValue(Constants.DOMAIN_UCC_OPT_NAME, optionValue);
						}
					} else if (null != option.getSrcFile()) {
						optionValue = FileUtils.getBase64FileBytes(option.getSrcFile());
						workingInstance.setValue(optionName, optionValue);
					} else {
						workingInstance.setValue(optionName, optionValue);
					}

				}
				// domain can be set as operation parameter, but may be
				// over-ridden
				if (null != this.getDomain() && null == operation.getDomain()) {
					if (log.isDebugEnabled()) {
						log.debug("option : name=domain, value=" + domain);
					}
					workingInstance.setValue(Constants.DOMAIN_OPT_NAME, domain);
					if (operation.isAMP){
						workingInstance.setValue(Constants.DOMAIN_UCC_OPT_NAME, domain);
					}
				}
				
				// recurse the schemaLoader model to create XML, assign to
				// operation.payload
				xmlString = workingInstance.generateDocumentString();
				
				// set payload to SOMA/AMP xml string
				operation.setPayload(xmlString);
				
				if (Constants.SET_FILES_CUSTOM_OP_NAME.equals(operation.getInvokedName())) {
					operation.getCustomOperation().deleteTempZipFile();
				}				
				
			}
		} catch (Exception ex) {
			getOperationChain().remove(operation);
			if (log.isDebugEnabled()) {
				log.error(ex.getMessage(), ex);
			} else {
				log.error(ex.getMessage());
			}
			if (failOnError) {
				System.exit(1);
			} 
		}
		return xmlString;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#postOperationXML()
	 */
	@Override
	public void postOperationXML() {
		Credentials credentials = getCredentials();

		for (Operation operation : getOperationChain()) {
			try {
				if (operation.getMemSafe()) {
					operation.setPayload(generateXMLInstance(operation));
				}
				if (!operation.customPostIntercept()){
					String xmlResponse = postXMLInstance(operation, credentials);
					operation.setResponse(xmlResponse);
					processResponse(operation);
				}
				if (operation.getMemSafe()) {
					operation.resetPayload();
				}
			} catch (Exception ex) {
				if (log.isDebugEnabled()) {
					log.error(ex.getMessage(), ex);
				} else {
					log.error(ex.getMessage());
				}
				if (failOnError) {
					System.exit(1);
				} 
			}
		}
		// Remove checkpoint if no errors have occurred.
		if (null != checkPointName) {
			removeCheckpoint();
		}
	}
	
	/**
	 * Poll for the desired waitFor result.
	 * 
	 * @param operation : the operation to poll.
	 */
	public void pollForResult(Operation operation) throws Exception{
		String responseString = null;
		String desiredResult = operation.getWaitFor();
		String resultLower = desiredResult.toLowerCase();
		String resultUpper = desiredResult.toUpperCase();
		String resultCapital = resultUpper.substring(0,1) + resultLower.substring(1,resultLower.length()-1);
		int numberOfPolls = 0;
		int waitTimeSeconds = operation.getWaitTime();
		int remainingTimeSeconds = waitTimeSeconds;
		int pollIntervalSeconds = operation.getPollIntMillis()/1000;
		String waitForString = ".*(" + resultLower + "|" + resultUpper + "|" + resultCapital + ").*";
		Pattern waitForPattern = Pattern.compile(waitForString);
		boolean matchResponse = false;
		while (!matchResponse && remainingTimeSeconds>0) {
			String responseXML = generateAndPost(operation);
			operation.response = responseXML;
			responseString = processResponse(operation);
			if (null == responseString) { 	
				String errorText = "Failed to parse DP response.";
				errorHandler(operation, errorText, org.apache.log4j.Level.FATAL);
			}
			Matcher waitForMatch = waitForPattern.matcher(responseString);
			matchResponse = waitForMatch.matches();
			numberOfPolls += 1;
			for (long stop = System.nanoTime()
					+ TimeUnit.MILLISECONDS.toNanos(operation.getPollIntMillis()); 
					stop > System.nanoTime();) {
			}
			remainingTimeSeconds = waitTimeSeconds - (numberOfPolls * pollIntervalSeconds);
		}
		if (!matchResponse && failOnError) {
			String errorText = "Failed to recieve the required \'" + operation.getWaitFor() 
					+ "\' response within a waitTime of " + waitTimeSeconds + " seconds.";
			errorHandler(operation, errorText, org.apache.log4j.Level.FATAL);
		}
	}
	

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#postXMLInstance(org.dpdirect.dpmgmt.DPDirect.Operation, org.dpdirect.utils.Credentials)
	 */
	@Override
	public String postXMLInstance(Operation operation, Credentials credentials) {
		// String endPoint = operation.getEndPoint();
		String xmlResponse = null;
		String xmlPayload = operation.getPayload();
		
		try {
			if (log.isDebugEnabled()) {
				log.debug("PostXML : " + operation.getName() + "  https://"
						+ getHostName() + ":" + getPort() + operation.getEndPoint());
			}
	
			if (log.isDebugEnabled()) {
				String payloadText = DocumentHelper.prettyPrintXML(xmlPayload);
				if (payloadText.length() > 4000) {
					payloadText = payloadText.substring(0, 2000)
							+ "... \n* truncated *";
				}
				log.debug("payload :\n" + payloadText);
			}
			
			if (logOutput) {
				if (null != operation.getParentOperation()){
					DPCustomOp customOp = operation.getParentOperation();
					if (!customOp.getPostLogged()){
						String opName = customOp.getName();
						log.info(opName);
						customOp.setPostLogged(true);
					}
				} else {
					String opName = operation.getInvokedName();
					log.info(opName);
				}
			}
			
			xmlResponse = PostXML.postTrusting(getHostName(), getPort(),
					operation.getEndPoint(), xmlPayload, credentials);
	
			if (log.isDebugEnabled()) {
				String responseText = DocumentHelper.prettyPrintXML(xmlResponse);
				if (responseText.length() > 4000) {
					responseText = responseText.substring(0, 2000)
							+ "... \n* truncated *";
				}
				log.debug("response :\n" + responseText);
			}
		} catch (Exception ex) {
			if (log.isDebugEnabled()) {
				log.error(ex.getMessage(), ex);
			} else {
				log.error(ex.getMessage());
			}
			if (failOnError) {
				System.exit(1);
			} 
		}
//        operation.setResponse(xmlResponse);
		return xmlResponse;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#parseResponseMsg(org.dpdirect.dpmgmt.DPDirect.Operation, boolean)
	 */
	@Override
	public String parseResponseMsg(Operation operation, boolean handleError) {
		List<Object> parseResult = new ArrayList<Object>();
	    org.apache.log4j.Level logLevel = org.apache.log4j.Level.INFO;
		String parsedText = null;
		try {
			parseResult = operation.getResponseParser().parseResponseMsg(operation
					.getResponse());
			logLevel = (org.apache.log4j.Level) parseResult.get(0);
			parsedText = (String) parseResult.get(1);
			
			if ((logLevel.toInt() > org.apache.log4j.Level.INFO_INT) && log.isDebugEnabled() && handleError) {
				logWarn(operation, parsedText);
			} else if ((logLevel.toInt() <= org.apache.log4j.Level.INFO_INT) 
					   && (operation.getSuppressResponse() != true 
				        || logLevel.toInt() > org.apache.log4j.Level.ERROR_INT)){
				logInfo(operation, parsedText);
			}
		} catch (Exception ex) {
			if (log.isDebugEnabled()) {
				log.error(ex.getMessage(), ex);
			} else {
				log.error(ex.getMessage());
			}
			if (failOnError) {
				System.exit(1);
			} 
		}
		/* Process errors and warnings */
		if ((logLevel.toInt() > org.apache.log4j.Level.INFO_INT) && handleError) {
			errorHandler(operation, parsedText, logLevel);
		}
		return parsedText;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#isSuccessResponse(org.dpdirect.dpmgmt.DPDirect.Operation)
	 */
	@Override
	public boolean isSuccessResponse(Operation operation) {
		boolean success = false;
		List<Object> parseResult = new ArrayList<Object>();
	    org.apache.log4j.Level logLevel = org.apache.log4j.Level.WARN;
		String parsedText = null;
		try {
			parseResult = operation.getResponseParser().parseResponseMsg(operation
					.getResponse());
			logLevel = (org.apache.log4j.Level) parseResult.get(0);
			parsedText = (String) parseResult.get(1);
			if (logLevel.toInt() <= org.apache.log4j.Level.INFO_INT) {
				success = true;
			} 
		} catch (Exception ex) {
			log.warn(ex.getMessage());
			log.debug(ex.getMessage(), ex);
		}
		return success;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#processResponse(org.dpdirect.dpmgmt.DPDirect.Operation)
	 */
	@Override
	public String processResponse(Operation operation) {
		org.apache.log4j.Level logLevel = org.apache.log4j.Level.INFO;
		String parsedText = null;

		try {
			parsedText = parseResponseMsg(operation, true);

			/*
			 * Save to file or print out to consoleMode
			 */
			if (Constants.DO_EXPORT_OP_NAME.equals(operation.getName())
					&& null != operation.getSrcDir()
					&& null != operation.getDestDir()) {
				// Directory retrieved via 'do-export'.. unzip and save to
				// nominated directory.
				String tempFileName = operation.getDestDir() + "/"
						+ Constants.DO_EXPORT_OP_NAME + ".zip";
				FileUtils.writeStringToFile(tempFileName, parsedText);
				FileUtils.extractZipDirectory(tempFileName,
						operation.getSrcDir(), operation.getDestDir(),
						operation.isOverwrite());
			}
		} catch (Exception ex) {
			if (log.isDebugEnabled()) {
				log.error(ex.getMessage(), ex);
			} else {
				log.error(ex.getMessage());
			}
			if (failOnError) {
				System.exit(1);
			} 
		}
		return parsedText;
	}

	/**
	 * Process and log error result returned from the parser. Will EXIT the
	 * program when critical errors are encountered.
	 * 
	 * @param operation
	 *            Operation : the current operation object.
	 * @param errorResponse
	 *            String : the current parsed result string returned.
	 * @param logLevel
	 *            org.apache.log4j.Level : the log level of the error.
	 * @throws Exception
	 *             - throws parse errors.
	 */
	protected void errorHandler(Operation operation, String errorResponse,
		 org.apache.log4j.Level logLevel) {
		try {
			String operationName = (null == operation) ? "" : (operation
					.getName() + " ");

			if (logLevel.toInt() >= org.apache.log4j.Level.FATAL_INT) {
				if (failOnError && operation.getFailFlag()) {
					if (null != checkPointName
							&& null != operation
							&& !Constants.SAVE_CHECKPOINT_OP_NAME
									.equals(operation.getName())
							&& !Constants.ROLLBACK_CHECKPOINT_OP_NAME
									.equals(operation.getName())) {
						// RESTORE CHECKPOINT.
						log.warn("errorResponse=" + errorResponse);
						log.warn("operation.getResponse()="
								+ operation.getResponse());
						log.info("Deployment Error... attempting rollback to checkpoint "
								+ checkPointName);
						boolean rolledBack = restoreCheckpoint();
						if (rolledBack) {
							removeCheckpoint();
							log.info("Rollback was successful.");
							System.exit(2);
						} else {
							log.info("Rollback was UNSUCCESSFUL!.");
						}
					} else {
						// STOP DEPLOYMENT.
						logError(operation, errorResponse);
					}
					System.exit(1);
				} else {
					logWarn(operation, errorResponse);
				}
			} else if (logLevel.toInt() >= org.apache.log4j.Level.WARN_INT) {
				logWarn(operation, errorResponse);
			}

		} catch (Exception ex) {
			if (log.isDebugEnabled()) {
				log.error(ex.getMessage(), ex);
			} else {
				log.error(ex.getMessage());
			}
			if (failOnError) {
				System.exit(1);
			} 
		}
	}
	
	protected void logInfo(Operation operation, String output){
		String opName = operation.getInvokedName();
		output = operation.customResultIntercept(output, true);
		if (!logOutput) {
			System.out.println(output);
		}
		else {
//			log.info(opName + " response:\n" + output);
//			System.out.println("Response: " + output);
			log.info(output);
		}
	}
	
	protected void logWarn(Operation operation, String errorResponse){
		if (!logOutput) {
			System.out.println("WARNING: " + errorResponse);
		}
		else {
			log.warn("errorResponse:\n" + errorResponse);
		}
	}
	
	protected void logError(Operation operation, String errorResponse){
		if (!logOutput) {
			System.out.println("ERROR: " + errorResponse);
		}
		else {
			log.error("errorResponse:\n" + errorResponse);
			if (errorResponse.trim().equals("")) {
				try {
					log.error(DocumentHelper.prettyPrintXML(operation.getResponse()));
				} catch (Exception e) {
					// do nothing
				}
			}
		}
	}

	/**
	 * Restore to checkpoint stored in the var checkPointName. checkPointName
	 * assigned if rollbackOnError is set to true. Invoked when when fatal
	 * (org.apache.log4j.Level.SEVERE) error encountered.
	 */
	protected boolean restoreCheckpoint() throws Exception {
		boolean success = false;
		if (checkPointName != null) {
			// create and post new operation to request rollback.
			Operation rollback = new Operation(
					Constants.ROLLBACK_CHECKPOINT_OP_NAME);
			rollback.addOption(Constants.CHK_NAME_OP_NAME, checkPointName);
			// schemaLoaderList.add(new SchemaLoader(
			// this.getClass().getResource(Constants.SOMA_MGMT_50_SCHEMA_PATH).toExternalForm()));

			generateXMLInstance(rollback);
			String xmlResponse = postXMLInstance(rollback, getCredentials());
			rollback.setResponse(xmlResponse);
			success = isSuccessResponse(rollback);
		}
		return success;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#generateAndPost(org.dpdirect.dpmgmt.DPDirect.Operation)
	 */
	@Override
	public String generateAndPost(Operation operation) {
		// Discern the target operation schema, and assign the DP device
		// endpoint.
		for (SchemaLoader loader : schemaLoaderList) {
			if (loader.nodeExists(operation.getName())) {
				operation.defineEndPoint(loader);
			}
		}

		String xmlResponse = null;
		generateXMLInstance(operation);
		if (operation.payload != null) {
			xmlResponse = postXMLInstance(operation, getCredentials());
		}
		
		return xmlResponse;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#getCredentialsFromNetrcConfig(java.lang.String)
	 */
	@Override
	public Credentials getCredentialsFromNetrcConfig(String hostName) {
		if (null != hostName) {
			try {
				BufferedReader reader = new BufferedReader(
						new FileReader(
								props.getProperty(DPDirectProperties.NETRC_FILE_PATH_KEY)));
				while (reader.ready()) {
					String line = reader.readLine();
					if (null != line && 0 < line.trim().length()
							&& !line.trim().startsWith("#")) {
						String[] tokens = line.split("\\s+");
						try {
							if ("machine".equalsIgnoreCase(tokens[0])
									&& "login".equalsIgnoreCase(tokens[2])
									&& "password".equalsIgnoreCase(tokens[4])) {
								if (hostName.equalsIgnoreCase(tokens[1])) {
									return new Credentials(tokens[3],
											tokens[5].toCharArray());
								}
							}
						} catch (Exception e) {
							// Ignore, continue to next line
						}
					}
				}
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setFirmware(java.lang.String)
	 */
	@Override
	public void setFirmware(String firmwareLevel) {
		int intLevel = DEFAULT_FIRMWARE_LEVEL;
		if (firmwareLevel.startsWith("default")) {
			intLevel = 0;
		} else if (firmwareLevel.startsWith("2018")) {
			intLevel = 2018;
		} else if (firmwareLevel.startsWith("8")) {
			intLevel = 8;
		} else if (firmwareLevel.startsWith("7")) {
			intLevel = 7;
		} else if (firmwareLevel.startsWith("6")) {
			intLevel = 6;
		} else if (firmwareLevel.startsWith("5")) {
			intLevel = 5;
		} else if (firmwareLevel.startsWith("4")) {
			intLevel = 4;
		} else if (firmwareLevel.startsWith("3")) {
			intLevel = 3;
		} else if (firmwareLevel.startsWith("2004")) {
			intLevel = 2004;
		} else if (firmwareLevel.startsWith("default")) {
			intLevel = 0;
		}
		this.userFirmwareLevel = firmwareLevel;
		this.firmwareLevel = intLevel;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#getNetrcFilePath()
	 */
	@Override
	public String getNetrcFilePath() {
		return netrcFilePath;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#setNetrcFilePath(java.lang.String)
	 */
	@Override
	public void setNetrcFilePath(String netrcFilePath) {
		this.netrcFilePath = netrcFilePath;
	}

	/**
	 * Inner class representing nested or stacked individual DP SOMA or AMP
	 * operations. Several operations may belong to a single DPDirect session.
	 */
	public class Operation {
		
		protected String name = null;

		protected DPCustomOp customOperation = null;
		
		protected DPCustomOp parentOperation = null;

		protected String domain = null;
		
		protected String srcFile = null;

		protected String destFile = null;

		protected String srcDir = null;

		protected String destDir = null;

		protected String endPoint = null;
		
		protected boolean isAMP = false;

		protected String payload = null;

		protected String response = null;

		protected boolean failFlag = true;
		
		protected String failState = null;
		
		protected String waitFor = null;
		
		protected int waitTimeSeconds = DEFAULT_WAIT_TIME_SECONDS;
		
		protected int pollIntMillis = DEFAULT_POLL_INT_MILLIS;

		protected String filter = null;

		protected String filterOut = null;

		protected boolean overwrite = true;
		
		protected boolean replace = true;
		
		protected boolean suppressResponse = false;
		
		protected boolean memSafe = false;

		protected List<Option> options = new ArrayList<Option>();
		
		/** Response parser. */
		protected ResponseParser responseParser = null;

		/**
		 * Default constructor for nested Operation class.
		 */
		public Operation() {
		}

		/**
		 * Named Constructor for for nested Operation class.
		 * 
		 * @param operationName
		 *            String : the name of this operation.
		 */
		public Operation(String operationName) {
			setName(operationName);
		}

		/**
		 * protected accessor method for nested Option class.
		 * 
		 * @return this class instance.
		 */
		protected Operation getOperation() {
			return this;
		}

		/**
		 * Set the name of this Operation instance.
		 * 
		 * Checks for custom operations such as "set-dir", "get-dir" etc.
		 * 
		 * @param name
		 *            the name of this operation.
		 */
		public void setName(String name) {
			if (DPCustomOp.isCustomOperation(name)){
				this.customOperation = new DPCustomOp(this, name);
				this.name = customOperation.getBaseName();
			} else {
				this.name = name;
			}
		}
		
		/**
		 * call the customOperation.customPostIntercept()
		 * @throws Exception 
		 */
		public boolean customPostIntercept() throws Exception {
			if (null != this.customOperation){
				return customOperation.customPostIntercept();
			}
			else if (null != this.waitFor){
				pollForResult(this);
				return true;
			}
			return false;
		}
		
		/**
		 * call the customOperation.customResultIntercept()
		 * @returns new result text if applicable
		 * @throws Exception 
		 */
		public String customResultIntercept(String response, boolean success){
			if (Constants.DO_IMPORT_OP_NAME.equalsIgnoreCase(this.getName()) 
					&& success
					&& Constants.PARSED_OUTPUT_OPT_NAME.equalsIgnoreCase(getOutputType())){
				//remove last object name - result accrues to all uploaded objects
				String pattern = "(\\s)(name=\\S+)(\\s)";
				response =  response.replaceAll(pattern, "$1"); 
				pattern = "(\\s)(class=\\S+)(\\s)";
				response = response.replaceAll(pattern, "$1"); 
			}
			return response;
		}
		
		/**
		 * Get the parentOperation object of this Operation instance.
		 */
		public DPCustomOp getParentOperation() {
			if (null != this.parentOperation){
				return this.parentOperation;
			} else if (null != this.customOperation){
				return this.customOperation;
			} else {
				return null;
			}
		}
		
		/**
		 * Set the parentOperation object of this Operation instance.
		 * @param parentOp
		 *            the custom DPCustomOp operation 'parent'.
		 */
		public void setParentOperation(DPCustomOp parentOp) {
			this.parentOperation = parentOp;
		}

		/**
		 * Set the domain name for this operation. An alternative to using the
		 * option setter. Many operations share the domain attribute.
		 * 
		 * @param domainName
		 *            String : the targeted domain name.
		 */
		public void setDomain(String domainName) {
			if (domainName != null
					&& (this.domain == null || !domainName.equals(this.domain))) {
				this.domain = domainName;
				Option option = createOption();
				option.setName(Constants.DOMAIN_OPT_NAME);
				option.setValue(domainName);
			}
		}
		public void updateDomainName(String domainName) {
			if (domainName != null
					&& (this.domain == null || !domainName.equals(this.domain))) {
				this.domain = domainName;
			}
		}

		/**
		 * Set the endPoint for this operation. allows the setting of a 'custom'
		 * endpoint, such as the /service/mgmt/2004 endpoint.
		 * 
		 * @param domainName
		 *            String : the targeted domain name.
		 */
		public void setEndPoint(String endPoint) {
			if (Constants.SOMA_MGMT_2004_SHORT.equalsIgnoreCase(endPoint.trim())) {
				this.endPoint = Constants.SOMA_MGMT_2004_URL;
				this.isAMP = false;
			} else if (Constants.SOMA_MGMT_SHORT.equalsIgnoreCase(endPoint.trim())) {
				this.endPoint = Constants.SOMA_MGMT_CURRENT_URL;
				this.isAMP = false;
			} else if (Constants.AMP_MGMT_SHORT.equalsIgnoreCase(endPoint.trim())) {
				this.endPoint = Constants.AMP_MGMT_30_URL;
				this.isAMP = true;
			} else {
				this.endPoint = endPoint;
				this.isAMP = endPoint.contains("mgmt/amp");
			}
		}

		/**
		 * Set the endPoint for this operation. allows the setting of a 'custom'
		 * endpoint, such as the /service/mgmt/2004 endpoint.
		 * 
		 * @param domainName
		 *            String : the targeted domain name.
		 */
		public void defineEndPoint(SchemaLoader loader) {
			if (null == this.getEndPoint()) {
				String schemaPath = loader.getSchemaFileURI();
				if (schemaPath.contains(Constants.SOMA_MGMT_SCHEMA_NAME)) {
					this.setEndPoint(Constants.SOMA_MGMT_CURRENT_URL);
					this.isAMP = false;
				} else if (schemaPath
						.contains(Constants.SOMA_MGMT_2004_SCHEMA_NAME)) {
					this.setEndPoint(Constants.SOMA_MGMT_2004_URL);
					this.isAMP = false;
				} else if (schemaPath
						.contains(Constants.AMP_MGMT_30_SCHEMA_NAME)) {
					this.setEndPoint(Constants.AMP_MGMT_30_URL);
					this.isAMP = true;
				} else if (schemaPath
						.contains(Constants.AMP_MGMT_40_SCHEMA_NAME)) {
					this.setEndPoint(Constants.AMP_MGMT_40_URL);
				} else if (schemaPath
						.contains(Constants.AMP_MGMT_DEFAULT_SCHEMA_NAME)) {
					this.setEndPoint(Constants.AMP_MGMT_DEFAULT_URL);
					this.isAMP = true;
				} else {
					this.setEndPoint(Constants.SOMA_MGMT_CURRENT_URL);
					this.isAMP = false;
				}
			}
		}

		/**
		 * Set the destination file path for download type operations such as
		 * 'get-file' and 'do-export'.
		 * 
		 * @param destFile
		 *            the target file path.
		 */
		public void setDestFile(String destFile) {
			if (null != destFile) {
				String optionName = this.getName();
				if (Constants.SET_FILE_OP_NAME.equals(optionName)) {
					optionName += ("@" + Constants.NAME_OPT_NAME);
					addOption(optionName, destFile);
				} else {
					this.destFile = destFile;
				}
			}
		}

		/**
		 * Set the source filename for upload type operations such as 'set-file'
		 * and 'do-import'.
		 * 
		 * @param srcFile
		 *            the source file path.
		 * @throws IOException
		 *             if there is an IO error.
		 */
		public void setSrcFile(String srcFile) throws IOException {
			this.srcFile = srcFile;
			String optionName = this.getName();
			if (Constants.GET_FILE_OP_NAME.equals(optionName)) {
				optionName += ("@" + Constants.NAME_OPT_NAME);
				addOption(optionName, srcFile);
			} else if (Constants.DO_IMPORT_OP_NAME.equals(optionName)) {
				optionName = Constants.INPUT_FILE_OPT_NAME;
				addOption(optionName, new File(srcFile));
			} else {
				addOption(optionName, new File(srcFile));
			}
		}
		
		public String getSrcFile() {
			return this.srcFile;
		}

		/**
		 * Set the destination directory for custom download operations
		 * 'get-dir' and 'get-files'.
		 * 
		 * @param destDir
		 *            the target directory.
		 */
		public void setDestDir(String destDir) {
//			addOption(Constants.DEST_DIR_OPT_NAME, destDir);
			setDestinationDirectory(destDir);
		}
		
		/**
		 * Set the destination directory for custom download operations
		 * 'get-dir' and 'get-files'.
		 * 
		 * @param destDir
		 *            the target directory.
		 */
		public void setDestinationDirectory(String destDir) {
			if (null != destDir) {
				this.destDir = FileUtils.normaliseDirPath(destDir, true);
			}
		}

		/**
		 * Set the source directory for custom upload operations 'set-dir' and
		 * 'set-files'.
		 * 
		 * @param sourceDir
		 *            String : the source directory - all contents are uploaded.
		 */
		public void setSrcDir(String sourceDir) {
//			addOption(Constants.SRC_DIR_OPT_NAME, sourceDir);
			updateSourceDir(sourceDir);
		}
		
		/**
		 * Set the source directory for custom upload operations 'set-dir' and
		 * 'set-files'.
		 * 
		 * @param sourceDir
		 *            String : the source directory - all contents are uploaded.
		 */
		public void updateSourceDir(String sourceDir) {
			if (null != sourceDir) {
				this.srcDir = FileUtils.normaliseDirPath(sourceDir, true);
			}
		}

		/**
		 * Set overwrite operations for upload and download type operations.
		 * 
		 * @param sourceDir
		 *            the source directory from which all files are uploaded.
		 */
		public void setOverwrite(boolean overwrite) {
			this.overwrite = overwrite;
			this.addOption(Constants.OVERWRITE_FILES_OP_NAME,
					Constants.TRUE_OPT_VALUE);
			this.addOption(Constants.OVERWRITE_OPT_NAME,
					Constants.TRUE_OPT_VALUE);
		}
		
		/**
		 * Set overwrite flag for upload and download type operations.
		 * 
		 * @param sourceDir
		 *            the boolean flag for over-write existing resources.
		 */
		public void updateOverwrite(boolean overwrite) {
			this.overwrite = overwrite;
		}
		
		/**
		 * Get the overwrite value
		 * 
		 * @return the overwrite value
		 */
		public boolean getOverwrite() {
			return this.overwrite;
		}
		
		/**
		 * Set replace flag for upload directory operations.
		 * 
		 * @param replace
		 *            the boolean flag for replace directory.
		 */
		public void setReplace(boolean replace) {
			this.replace = replace;
		}
		
		/**
		 * Get the replace value
		 * 
		 * @return the replace value
		 */
		public boolean getReplace() {
			return this.replace;
		}
		
		/**
		 * Set the memSafe switch
		 * 
		 * @param memSafe
		 *            whether payload should be prebuilt - 
		 *            true (xml pre-verified) or false (smaller memory footprint)
		 */
		public void setMemSafe(boolean memSafe) {
			this.memSafe = memSafe;
		}
		
		/**
		 * Get the memSafe switch
		 * 
		 * @return the memSafe value
		 */
		public boolean getMemSafe() {
			return this.memSafe;
		}
		
		/**
		 * Set the waitFor value
		 * 
		 * @param result
		 *            operation result to poll for.
		 */
		public void setWaitFor(String result) {
			this.waitFor = result;
		}
		
		/**
		 * Get the waitFor value
		 * 
		 * @return the waitFor value
		 */
		public String getWaitFor() {
			return this.waitFor;
		}
		
		/**
		 * Set the waitFor time
		 * 
		 * @param timeSeconds
		 *            operation waitFor result wait time.
		 */
		public void setWaitTime(int timeSeconds) {
			this.waitTimeSeconds = timeSeconds;
		}
		
		/**
		 * Get the waitFor time in Seconds
		 * 
		 * @return the waitFor time in Seconds
		 */
		public int getWaitTime() {
			return this.waitTimeSeconds;
		}
		
		/**
		 * @return the logPollIntMillis
		 */
		public int getPollIntMillis() {
			return pollIntMillis;
		}

		/**
		 * @param logPollIntMillis
		 *            the logPollIntMillis to set
		 */
		public void setPollIntMillis(int logPollIntMillis) {
			this.pollIntMillis = logPollIntMillis;
		}
		
		/**
		 * @return the customOperation
		 */
		public DPCustomOp getCustomOperation() {
			return customOperation;
		}
		
		/**
		 * @return the operation polls
		 */
		public boolean isPolling() {
			if (null != customOperation){
				return customOperation.isPolling();
			}
			else {
				return false;
			}
		}

		/**
		 * @param operation
		 *            add operation to operation chain
		 */
		public void addToOperationChain(Operation operation) {
			getOperationChain().add(operation);
		}

		/**
		 * @param index
		 * @param operation
		 *            add operation to operation chain
		 */
		public void addToOperationChain(int i, Operation operation) {
			getOperationChain().add(i, operation);
		}
		
		/**
		 * @return the DPDirectBase instance
		 */
		public DPDirectBase getOuterInstance() {
			return getDPDInstance();
		}

		/**
		 * @return the payload
		 */
		public String getPayload() {
			return payload;
		}

		/**
		 * @param payload
		 *            the payload to set
		 */
		public void setPayload(String payload) {
			this.payload = payload;
		}
		
		/**
		 * Reset the payload to null
		 */
		public void resetPayload() {
			this.payload = null;
		}


		/**
		 * @return the response
		 */
		public String getResponse() {
			return response;
		}

		/**
		 * @param response
		 *            the response to set
		 */
		public void setResponse(String response) {
			this.response = response;
		}
		
		/**
		 * @return the response
		 */
		public boolean getSuppressResponse() {
			return suppressResponse;
		}

		/**
		 * @param response
		 *            the response to set
		 */
		public void setSuppressResponse(boolean suppress) {
			this.suppressResponse = suppress;
		}
		
		/**
		 * @return the failState
		 */
		public String getFailState() {
			return failState;
		}

		/**
		 * @param failState
		 *            the response to trigger fail
		 */
		public void setFailState(String failString) {
			this.failState = failString;
		}
			
		/**
		 * @return the failFlag
		 */
		public boolean getFailFlag() {
			return failFlag;
		}
		
		/**
		 * @param failFlag
		 *            does the response fail on error
		 */
		public void setFailFlag(boolean flag) {
			this.failFlag = flag;
		}
		
		/**
		 * @param failFlag
		 *            does the response fail on error
		 */
		public void setFailOnError(boolean flag) {
			setFailFlag(flag);
		}

		/**
		 * @return the options
		 */
		public List<Option> getOptions() {
			return options;
		}

		/**
		 * @param options
		 *            the options to set
		 */
		public void setOptions(List<Option> options) {
			this.options = options;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * @return the name as invoked - custom, SOMA or AMP
		 */
		public String getInvokedName() {
			if (null != customOperation){
				return customOperation.getName();
			} else if (null != parentOperation) {
				return parentOperation.getName();
			}
			else {
				return name;
			}
		}

		/**
		 * @return the domain
		 */
		public String getDomain() {
			return domain;
		}
		
		/**
		 * @return the domain
		 */
		public String getEffectiveDomain() {
			if (null != this.domain){
				return domain;
			}
			else {
				return getDefaultDomain();
			}
		}

		/**
		 * @return the destFile
		 */
		public String getDestFile() {
			return destFile;
		}

		/**
		 * @return the srcDir
		 */
		public String getSrcDir() {
			return srcDir;
		}

		/**
		 * @return the destDir
		 */
		public String getDestDir() {
			return destDir;
		}

		/**
		 * @return the overwrite
		 */
		public boolean isOverwrite() {
			return overwrite;
		}
		
		/**
		 * @return the replace flag
		 */
		public boolean isReplace() {
			return replace;
		}

		/**
		 * @return the endPoint
		 */
		public String getEndPoint() {
			return endPoint;
		}
		

		/**
		 * @return the typeFilter
		 */
		public String getFilter() {
			return filter;
		}

		/**
		 * @return the negative typeFilter
		 */
		public String getFilterOut() {
			return filterOut;
		}

		/**
		 * @param filter
		 *            the typeFilter to set
		 */
		public void setFilter(String filter) {
			if (null != this.filter) {
				this.filter += "|" + filter;
			}
			else {
				this.filter = filter;
			}
		}

		/**
		 * @param filterOut
		 *            the negative typeFilter to set
		 */
		public void setFilterOut(String filterOut) {
			if (null != this.filterOut) {
				this.filterOut += "|" + filterOut;
			}
			else {
				this.filterOut = filterOut;
			}
		}
		
		public List<Operation> getOperationChain(){
			return getOuterInstance().getOperationChain();
		}
		
		/**
		 * @return the responseParser for the operation
		 */
		public ResponseParser getResponseParser() {
			if (null == this.responseParser) {
				setResponseParser();
			}
			return this.responseParser;
		}

		/**
		 * Set the responseParser for the operation
		 */
		public void setResponseParser() {
			this.responseParser = new ResponseParser();
			responseParser.setOutputType(getOutputType());
			responseParser.setOutputFile(this.getDestFile());
			responseParser.setFailureState(this.getFailState());
			responseParser.setFilter(this.getFilter());
			responseParser.setFilterOut(this.getFilterOut());
			responseParser.setSuppressResponse(this.getSuppressResponse());
		}

		/**
		 * Default method to create a nested option for this operation.
		 */
		public Option createOption() {
			Option option = new Option();
			options.add(option);
			return option;
		}

		/**
		 * Default Ant method to create a nested option for this operation.
		 * 
		 * @param name
		 *            the name of the option.
		 */
		public void addOption(String name) {
			Option option = createOption();
			option.setName(name);
		}

		/**
		 * Utility method to create nested operation options from name/value
		 * pair.
		 * 
		 * @param optionName
		 *            the name of the option.
		 * @param optionValue
		 *            the value of the option.
		 */
		public void addOption(String optionName, String optionValue) {
			Option option = createOption();
			option.setName(optionName);
			option.setValue(optionValue);
		}
		
		/**
		 * Utility method to create custom operation options from name/value
		 * pair.
		 * 
		 * @param optionName
		 *            the name of the option.
		 * @param optionValue
		 *            the value of the option.
		 */
		public void addCustomOptions(String optionName, String optionValue) {
			if (null != this.customOperation){
				customOperation.addCustomOptions(optionName, optionValue);
			}
		}
		
		/**
		 * Utility method to create funtional options from name/value
		 * pair.
		 * 
		 * @param optionName
		 *            the name of the option.
		 * @param optionValue
		 *            the value of the option.
		 */
		public void addFunctionalOptions(String optionName, String optionValue) {
			if (Constants.END_POINT_OPT_NAME.equalsIgnoreCase(optionName)) {
				setEndPoint(optionValue);
			} else if (Constants.FILTER_OPT_NAME.equalsIgnoreCase(optionName)) {
				setFilter(optionValue);
			} else if (Constants.FILTER_OUT_OPT_NAME.equalsIgnoreCase(optionName)) {
				setFilterOut(optionValue);
			} else if (Constants.DEST_DIR_OPT_NAME.equalsIgnoreCase(optionName)) {
				getOperation().setDestDir(optionValue);
			} else if (Constants.SRC_DIR_OPT_NAME.equalsIgnoreCase(optionName)) {
				getOperation().setSrcDir(optionValue);
			} else if (Constants.DEST_FILE_OPT_NAME.equalsIgnoreCase(optionName)) {
				getOperation().setDestFile(optionValue);
			} else if (Constants.SRC_FILE_OPT_NAME.equalsIgnoreCase(optionName)) {
				try {
					setSrcFile(optionValue);
				} catch (IOException e) {
					if (log.isDebugEnabled()) {
						log.error("Failed to set src file. "
								+ e.getMessage());
					} else {
						log.error("Failed to set src file. "
								+ e.getMessage(), e);
					}
					if (getOuterInstance().failOnError) {
						System.exit(1);
					} 
				}
			} else if (Constants.FAIL_STATE_OPT_NAME.equalsIgnoreCase(optionName)) {
				setFailState(optionValue);
			} else if (Constants.DOMAIN_OPT_NAME.equalsIgnoreCase(optionName)) {
				getOperation().updateDomainName(optionName);
			} else if (Constants.OVERWRITE_OPT_NAME.equalsIgnoreCase(optionName)) {
				if (null != optionValue) {
					getOperation().updateOverwrite(
						Constants.TRUE_OPT_VALUE.equals(optionValue.trim().toLowerCase()));
				}
			}
		}

		/**
		 * Utility method to create nested operation options from name/value
		 * pair.
		 * 
		 * @param optionName
		 *            the name of the option.
		 * @param srcFile
		 *            the path of a source file to base64 encode and set as the
		 *            option value.
		 * @throws IOException
		 */
		public void addOption(String optionName, File srcFile)
				throws IOException {
			Option option = createOption();
			option.setName(optionName);
			option.setSrcFile(srcFile.getPath());
			this.srcFile = srcFile.getAbsolutePath();
		}

		/**
		 * Gets an option value for the current list of options.
		 * 
		 * @param name
		 *            the name of the option.
		 * @return the most recently added value for the option, or null if there is no such option.
		 */
		public String getOptionValue(String optionName) {
			ListIterator<Option> i = getOptions().listIterator(getOptions().size());
			while (i.hasPrevious()) {
				Option opt = i.previous();
				if (opt.name.equals(optionName)) {
					return opt.value;
				}
			}
			return null;
		}
		
		/**
		 * Gets an option from the current list of options.
		 * 
		 * @param name
		 *            the name of the option.
		 * @return the most recently added option object, or null if there is no such option.
		 */
		public Option getOption(String optionName) {
			ListIterator<Option> i = getOptions().listIterator(getOptions().size());
			while (i.hasPrevious()) {
				Option opt = i.previous();
				if (opt.name.equals(optionName)) {
					return opt;
				}
			}
			return null;
		}

		/**
		 * Inner class of nested options representing attribute or element
		 * values for a SOMA or AMP operation. Several options may belong to a
		 * single operation.
		 */
		public class Option {

			protected String name = null;

			protected String value = null;

			protected String srcFile = null;

			/**
			 * Default constructor for nested Option class.
			 */
			public Option() {
			}

			/**
			 * Sets the option name.
			 * 
			 * @param name
			 *            the option name to set.
			 */
			public void setName(String name) {
				this.name = name;
				// The ant task can contain name and value attributes in any
				// order so setValue() and setName() might be called in
				// different
				// orders. This method fires a common operation when the bean
				// state is sufficient to update the parent
				// object.
				if (null != this.getValue()) {
					getOperation().addFunctionalOptions(this.getName(), this.getValue());
					getOperation().addCustomOptions(this.getName(), this.getValue());
				}
			}

			/**
			 * Sets the option value.
			 * 
			 * @param value
			 *            the value to set.
			 */
			public void setValue(String value) {
				this.value = value;
				// The ant task can contain name and value attributes in any
				// order so setValue() and setName() might be called in
				// different
				// orders. This method fires a common operation when the bean
				// state is sufficient to update the parent
				// object.
				if (null != getName()) {
					getOperation().addFunctionalOptions(this.getName(), this.getValue());
					getOperation().addCustomOptions(this.getName(), this.getValue());
				}
			}

			/**
			 * Sets the value of this Option as the base64 encoded contents of a
			 * given source file.
			 * 
			 * @param srcFile
			 *            the path to the source file.
			 * @throws IOException
			 */
			public void setSrcFile(String srcFile) throws IOException {
				if (null == this.getName()) {
					this.setName(getOperation().getName());
				}
				this.srcFile = srcFile;
			}

			/**
			 * @return the name
			 */
			public String getName() {
				return name;
			}
			
			/**
			 * @return the value
			 */
			public void resetValue() {
				value = null;
			}

			/**
			 * @return the value
			 */
			public String getValue() {
				return value;
			}

			/**
			 * @return the srcFile
			 */
			public String getSrcFile() {
				return srcFile;
			}
		}
	}

}
