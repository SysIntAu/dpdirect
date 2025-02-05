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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.apache.log4j.Logger;

import org.dpdirect.dpmgmt.Operation.Option;
import org.dpdirect.schema.DocumentHelper;
import org.dpdirect.schema.SchemaLoader;
import org.dpdirect.utils.Credentials;
import org.dpdirect.utils.DPDirectProperties;
import org.dpdirect.utils.FileUtils;
import org.dpdirect.utils.PostXML;

import static org.dpdirect.dpmgmt.Defaults.DEFAULT_FIRMWARE_LEVEL;

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
		getOperationChain().add(operation);
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#addToOperationChain(org.dpdirect.dpmgmt.DPDirect.Operation)
	 */
	public void addToOperationChain(int i, Operation operation) {
		getOperationChain().add(i, operation);
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
	
	@Override
	public void setSchema() {
		log.debug("firmwareLevel: " + firmwareLevel);
		log.debug("userFirmwareLevel: " + userFirmwareLevel);

		try {
			if (schemaLoaderList.isEmpty()) {
				if (firmwareLevel >= 3) {
					log.info("Using custom schema paths for firmware level " + firmwareLevel);
					addSchema(Constants.MGMT_SCHEMAS_DIR + "/" + userFirmwareLevel + "/" + Constants.SOMA_MGMT_SCHEMA_NAME,
							"SOMAInstance", null);
					addSchema(Constants.MGMT_SCHEMAS_DIR + "/" + userFirmwareLevel + "/" + Constants.AMP_MGMT_DEFAULT_SCHEMA_NAME,
							"AMPInstance", null);
				} else {
					log.info("Using default schema paths for firmware level " + firmwareLevel);
					addSchema(Constants.SOMA_MGMT_DEFAULT_SCHEMA_PATH, "SOMAInstance", null);
					addSchema(Constants.AMP_MGMT_DEFAULT_SCHEMA_PATH, "AMPInstance", null);
				}
			} else {
				log.info("Schemas already loaded. Skipping schema initialization.");
			}
		} catch (Exception ex) {
			if (!failOnError && !log.isDebugEnabled()) {
				log.error(ex.getMessage());
			} else {
				log.error("Exception occurred while setting schemas", ex);
			}
		}
	}

	@Override
	public void setSchema(String schemaPath) {
		try {
			if (Constants.SOMA_MGMT_2004_SHORT.equalsIgnoreCase(schemaPath)) {
				addSchema(Constants.SOMA_MGMT_DEFAULT_SCHEMA_PATH, "SOMAInstance", 0);
			} else {
				addSchema(Constants.AMP_MGMT_DEFAULT_SCHEMA_PATH, "AMPInstance", 0);
				addSchema(Constants.SOMA_MGMT_DEFAULT_SCHEMA_PATH, "SOMAInstance", 0);
			}
		} catch (Exception ex) {
			if (!failOnError && !log.isDebugEnabled()) {
				log.error(ex.getMessage());
			} else {
				log.error("Exception occurred while setting schema with path: " + schemaPath, ex);
			}
		}
	}

	/**
	 * Loads a schema resource from the classpath and adds it to schemaLoaderList.
	 *
	 * @param path   The resource path to load.
	 * @param label  A label for logging (e.g., "SOMAInstance" or "AMPInstance").
	 * @param index  If non-null, the schema is inserted at this index; otherwise, it’s appended.
	 * @throws FileNotFoundException if the resource cannot be found.
	 */
	private void addSchema(String path, String label, Integer index) throws Exception {
		URL url = getClass().getResource(path);
		if (url == null) {
			String errMsg = "Schema resource not found at path: " + path;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}
		SchemaLoader loader = new SchemaLoader(url.toExternalForm());
		if (index != null) {
			schemaLoaderList.add(index, loader);
		} else {
			schemaLoaderList.add(loader);
		}
		log.info(label + " schema loaded successfully. URI: " + loader.getSchemaURI());
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
				|| !propFileName.endsWith(PROP_SUFFIX)) {
			propFileName = propFileName + PROP_SUFFIX;
		}
		try {
			File propFile = new File(propFileName);
			String propFilePath = propFile.getParent();
			if (propFilePath == null && !propFile.exists()) {
				String filePath = FileUtils.class.getProtectionDomain()
						.getCodeSource().getLocation().getPath();
				if (System.getProperty("os.name").startsWith("Windows")){
					filePath = filePath.substring(1);
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
	 * @param isLogged the output be logged.
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
			Operation operation = new Operation(this, Constants.SAVE_CHECKPOINT_OP_NAME);
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
		Operation removeCheckpoint = new Operation(this, Constants.REMOVE_CHECKPOINT_OP_NAME);
		removeCheckpoint.addOption(Constants.CHK_NAME_OP_NAME, checkPointName);

		String xmlResponse = generateAndPost(removeCheckpoint);
		removeCheckpoint.setResponse(xmlResponse);
		parseResponseMsg(removeCheckpoint, false);
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#createOperation()
	 */
	@Override
	public Operation createOperation() {
		Operation operation = new Operation(this);
		addToOperationChain(operation);
		return operation;
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#createOperation(java.lang.String)
	 */
	@Override
	public Operation createOperation(String operationName) {
		Operation operation = new Operation(this, operationName);
		addToOperationChain(operation);
		return operation;
	}

	public Operation newOperation(String operationName) {
		return new Operation(this, operationName);
	}

	/* (non-Javadoc)
	 * @see org.dpdirect.dpmgmt.DPDirectBaseInterface#addOperation(java.lang.String)
	 */
	@Override
	public void addOperation(String operationName) {
		Operation operation = new Operation(this, operationName);
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
	public void pollForResult(Operation operation) throws Exception {
		String responseString = null;
		int numberOfPolls = 0;
		int waitTimeSeconds = operation.getWaitTime();
		int remainingTimeSeconds = waitTimeSeconds;
		int pollIntervalSeconds = operation.getPollIntMillis()/1000;
		boolean matchResponse = false;
		
		String waitFor = operation.getWaitFor();
		String waitForXPath = operation.getWaitForXPath();
		
		Pattern waitForPattern = null;
		if (waitFor != null) {
			String resultLower = waitFor.toLowerCase();
			String resultUpper = waitFor.toUpperCase();
			String resultCapital = resultUpper.charAt(0) + resultLower.substring(1,resultLower.length()-1);
			String waitForString = ".*(" + resultLower + "|" + resultUpper + "|" + resultCapital + ").*";
			waitForPattern = Pattern.compile(waitForString);
		}

		if (waitForXPath != null) {
			try {
				validateXPathExpression(waitForXPath);
			} catch (XPathExpressionException ex) {
				String errorText = "Failed to validate XPath expression - " + ex.getMessage();
				errorHandler(operation, errorText, org.apache.log4j.Level.FATAL);
			}
		}
	
		while (!matchResponse && remainingTimeSeconds > 0) {
			String responseXML = generateAndPost(operation); 
			operation.response = responseXML;
			responseString = processResponse(operation);
			
			if (null == responseString) {
				String errorText = "Failed to parse DP response.";
				errorHandler(operation, errorText, org.apache.log4j.Level.FATAL);
			}

			if (waitFor != null) {
				Matcher waitForMatch = waitForPattern.matcher(responseString);
				matchResponse = waitForMatch.matches();
			} else if (waitForXPath != null) {
				matchResponse = evaluateXPath(responseXML, waitForXPath);
			}
	
			numberOfPolls++;
			for (long stop = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(operation.getPollIntMillis()); 
				 stop > System.nanoTime();) { }
			remainingTimeSeconds = waitTimeSeconds - (numberOfPolls * pollIntervalSeconds);
		}
	
		if (!matchResponse && failOnError) {
			String errorText = "Failed to receive ";
			if (waitFor != null) {
				errorText += "the required '" + waitFor + "'";
			} else if (waitForXPath != null) {
				errorText += "matching XPath '" + waitForXPath + "'";
			}
			errorText += " response within " + waitTimeSeconds + " seconds.";
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
					   && (!operation.getSuppressResponse()
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
	 * Validate an XPath expression.
	 * 
	 * @param xpath String : the XPath expression to validate.
	 */
	private void validateXPathExpression(String xpath) throws XPathExpressionException {
		if (xpath == null || xpath.trim().isEmpty()) {
			throw new XPathExpressionException("XPath expression is null or empty");
		}

		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			xPath.compile(xpath);
		} catch (XPathExpressionException e) {
			log.error("Invalid XPath syntax: " + xpath + " - " + e.getMessage());
			throw e;
		}
	}

	/**
	 * Evaluate an XPath expression against an XML string.
	 * 
	 * @param xml
	 *            String : the XML string to evaluate.
	 * @param xpath
	 *            String : the XPath expression to evaluate.
	 * @return boolean : true if the XPath expression evaluates to true.
	 */
	private boolean evaluateXPath(String xml, String xpath) throws XPathExpressionException {

		log.info("Evaluating XPath: " + xpath);
		log.debug("Against XML: " + xml);

		validateXPathExpression(xpath);

		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			xPath.compile(xpath);
	
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(xml)));
			NodeList nodes = (NodeList)xPath.evaluate(xpath, doc, XPathConstants.NODESET);

			log.info("XPath evaluation result: " + nodes.getLength() + " (" + (nodes.getLength() > 0) + ")");

			return nodes.getLength() > 0;
		} catch (Exception e) {
			log.error("XPath evaluation failed: " + e.getMessage());
			return false;
		}
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
					if (null != checkPointName && !Constants.SAVE_CHECKPOINT_OP_NAME
                            .equals(operation.getName()) && !Constants.ROLLBACK_CHECKPOINT_OP_NAME
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
			if (errorResponse.trim().isEmpty()) {
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
	protected boolean restoreCheckpoint() {
		boolean success = false;
		if (checkPointName != null) {
			// create and post new operation to request rollback.
			Operation rollback = new Operation(this, Constants.ROLLBACK_CHECKPOINT_OP_NAME);
			rollback.addOption(Constants.CHK_NAME_OP_NAME, checkPointName);

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
					if (null != line && !line.trim().isEmpty()
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



}
