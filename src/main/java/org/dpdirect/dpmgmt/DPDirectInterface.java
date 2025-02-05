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
 
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.dpdirect.dpmgmt.Operation;
import org.dpdirect.utils.Credentials;

public interface DPDirectInterface {
	
	/**
	 * Executes the task.
	 * 
	 * @throws BuildException
	 *             if there is a fatal error running the task.
	 */
	public abstract void execute() throws BuildException;
	
	/**
	 * Returnss operationChain list
	 */
	public abstract List<? extends Operation> getOperationChain();

	/**
	 * Clears operationChain list
	 */
	public abstract void resetOperationChain();

	/**
	 * Clears loaded SchemaLoader classes
	 */
	public abstract void resetSchemas();

	/**
	 * Set the default schema if not already set. These versions are bundled in
	 * the jar.
	 */
	public abstract void setSchema();

	/**
	 * Setter to load an alternative or updated SOMA schema.
	 * 
	 * @param schemaPath
	 *            String : the path to the SOMA schema
	 */
	public abstract void setSchema(String schemaPath);

	/**
	 * Read and process a named properties file. Sets global properties.
	 * 
	 * @param propFileName
	 *            String : the name of the properties file.
	 */
	public abstract void processPropertiesFile(String propFileName);

	/**
	 * Setter global options via name value pairs. Used by command line
	 * invocation.
	 * 
	 * @param name
	 *            the option name
	 * @param value
	 *            the option value
	 */
	public abstract void setGlobalOption(String name, String value);

	/**
	 * Get the output type.
	 */
	public abstract String getOutputType();

	/**
	 * Sets the output type.
	 * 
	 * @param type
	 *            the output type - of XML, LINES or PARSED.
	 */
	public abstract void setOutputType(String type);

	/**
	 * Sets the target DP device hostName.
	 * 
	 * @param hostName
	 *            the target hostName.
	 */
	public abstract void setHostName(String hostName);

	/**
	 * Sets the target DP device XML interface port.
	 * 
	 * @param port
	 *            the target port number.
	 */
	public abstract void setPort(String port);

	/**
	 * @return the port
	 */
	public abstract String getPort();

	/**
	 * Sets the target DP device userName.
	 * 
	 * @param userName
	 *            the target DP device userName.
	 */
	public abstract void setUserName(String userName);

	/**
	 * Sets the target DP device password for the authorised user.
	 * 
	 * @param userName
	 *            the target DP device password for the authorised user.
	 */
	public abstract void setUserPassword(String password);

	public abstract Credentials getCredentials();

	public abstract void setCredentials(Credentials credentials);

	public abstract String getHostName();

	/**
	 * Sets the default DP domain for a set of operations. Individual operations
	 * may assume the default domain, or explicitly over-ride it.
	 * 
	 * @param domain
	 *            the default target DP domain for this set of operations.
	 */
	public abstract void setDomain(String domain);

	public abstract String getDomain();

	/**
	 * Sets the log-level and verbosity of output.
	 * 
	 * @param verboseOutput
	 *            Produce verbose output - 'true' or 'false'.
	 */
	public abstract void setVerbose(String verboseOutput);

	/**
	 * Sets the log-level.
	 * 
	 * @param debugOutput
	 *            Produce debug output - 'true' or 'false'.
	 */
	public abstract void setDebug(String debugOutput);

	/**
	 * Sets the failOnError option. Setting to 'true' fails and immediately
	 * ceases the build when errors are returned.
	 * 
	 * @param failOnError
	 *            true if the build should cease when failures are returned;
	 *            false otherwise.
	 */
	public abstract void setFailOnError(boolean failOnError);

	/**
	 * Setter to save a checkpoint and rollback if the build should fail.
	 * 
	 * @param enableRollback
	 *            boolean : true to save a checkpoint and rollback in case of
	 *            failure.
	 */
	public abstract void setRollbackOnError(boolean enableRollback);

	/**
	 * Setter to save a checkpoint and rollback if the build should fail.
	 * 
	 * @param enableRollback
	 *            boolean : true to save a checkpoint and rollback in case of
	 *            failure.
	 */
	public abstract void removeCheckpoint();

	/**
	 * Default method to create a nested operation.
	 */
	public abstract Operation createOperation();

	/**
	 * Method to create a nested operation by name.
	 * 
	 * @param operationName
	 *            String : name corresponding to a valid SOMA or AMP operation.
	 * @return operation created.
	 */
	public abstract Operation createOperation(String operationName);

	/**
	 * Add a nested operation by name. Utilised by Ant task for nested
	 * instances.
	 * 
	 * @param operationName
	 *            String : name corresponding to a valid SOMA or AMP operation.
	 */
	public abstract void addOperation(String operationName);

	/**
	 * Generate the SOMA and AMP XML for each operation.
	 * 
	 * @param operation
	 *            Operation : the current operation object.
	 * @throws Exception
	 *             - if valid XML cannot be generated.
	 */
	public abstract String generateXMLInstance(Operation operation);

	/**
	 * Iterate through the SOMA and AMP XML for each operation, post and parse
	 * response. XML will have been validated at this point. Will fail the build
	 * and exit(1) upon error if 'failOnError is set to true (default=true).
	 * Will restore to checkpoint and exit(2) upon error if rollbackOnError is
	 * set to true (default=false).
	 */
	public abstract void postOperationXML();

	public abstract String parseResponseMsg(Operation operation,
			boolean handleError);

	public abstract boolean isSuccessResponse(Operation operation);

	/**
	 * Process the XML response for an operation.
	 * 
	 * @param operation
	 *            Operation : the current operation object.
	 * @return parsed response.
	 */
	public abstract String processResponse(Operation operation);

	/**
	 * Generate the XML for the provided operation and post to device.
	 * 
	 * @param operation
	 *            the current operation object
	 */
	public abstract String generateAndPost(Operation operation);

	/**
	 * Post SOMA or AMP XML for an individual operation.
	 * 
	 * @param operation
	 *            the current operation object.
	 * @param credentials
	 *            authorised username and password for the device.
	 * @throws Exception
	 *             if unable to post, or other critical errors encountered.
	 */
	public abstract String postXMLInstance(Operation operation,
			Credentials credentials);

	/**
	 * Gets DataPower logon credentials from a NETRC file (on windows "_netrc"
	 * located in the user home drive).
	 * 
	 * @param hostName
	 *            the host name to match in the NETRC file.
	 * 
	 * @return a Base64 encoded basic authorisation header string for the given
	 *         host; or null if there is no NETRC file or no entry in the NETRC
	 *         file for the host.
	 */
	public abstract Credentials getCredentialsFromNetrcConfig(String hostName);

	/**
	 * @param firmwareLevel
	 *            the firmwareLevel to set
	 */
	public abstract void setFirmware(String firmwareLevel);

	/**
	 * @return the netrcFilePath
	 */
	public abstract String getNetrcFilePath();

	/**
	 * @param netrcFilePath
	 *            the netrcFilePath to set
	 */
	public abstract void setNetrcFilePath(String netrcFilePath);

}