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

public interface DPDirectInterface {
	
	/**
	 * Executes the task.
	 * 
	 * @throws BuildException
	 *             if there is a fatal error running the task.
	 */
	public abstract void execute() throws BuildException;


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
	 * Sets the target DP device userName.
	 * 
	 * @param userName
	 *            the target DP device userName.
	 */
	public abstract void setUserName(String userName);

	/**
	 * Sets the target DP device password for the authorised user.
	 * 
	 * @param password
	 *            the target DP device password for the authorised user.
	 */
	public abstract void setUserPassword(String password);

	/**
	 * Sets the default DP domain for a set of operations. Individual operations
	 * may assume the default domain, or explicitly over-ride it.
	 * 
	 * @param domain
	 *            the default target DP domain for this set of operations.
	 */
	public abstract void setDomain(String domain);

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
	 * Sets the output type.
	 * 
	 * @param type
	 *            the output type - of XML, LINES or PARSED.
	 */
	public abstract void setOutputType(String type);

	/**
	 * Sets the log-level and verbosity of output.
	 * 
	 * @param verboseOutput
	 *            Produce verbose output - 'true' or 'false'.
	 */
	public abstract void setVerbose(String verboseOutput);

	/**
	 * @param firmwareLevel
	 *            the firmwareLevel to set
	 */
	public abstract void setFirmware(String firmwareLevel);

	/**
	 * Set the default schema if not already set. These versions are bundled in
	 * the jar.
	 */
	public abstract void setSchema(String schema);

	/**
	 * Default method to create a nested operation.
	 */
	public abstract Operation createOperation();

}