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
 
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.dpdirect.dpmgmt.DPDirectBase;
import org.dpdirect.dpmgmt.DPDirectBase.Operation;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.dpdirect.utils.Credentials;



/**
 * Class for the management of IBM DataPower device via the and and the XML management
 * interface.
 * The purpose of this DynamicPoxy is to decouple the Ant TASK import from the cmd-line 
 * invocation of the program, and thus avoid an unnecessary import of the Ant lib.
 * 
 * Ant task for IBM DataPower management.
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
 * that corresponds to a valid SOMA or AMP operation.
 * 
 * See the method text for antHelp() for usage details.
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
public class DPDirectTask extends Task implements DPDirectInterface {
	
	private DPDirectBase base = new
			DPDirectBase();
	
	public DPDirectTask() {}

	@Override
	public void execute() throws BuildException {
		base.execute();
	}
	
	@Override
	public List<Operation> getOperationChain() {
		return (List<Operation>)base.getOperationChain();
	}

	@Override
	public void resetOperationChain() {
		base.resetOperationChain();
	}

	@Override
	public void resetSchemas() {
		base.resetSchemas();
	}

	@Override
	public void setSchema() {
		base.setSchema();
	}

	@Override
	public void setSchema(String schemaPath) {
		base.setSchema(schemaPath);
	}

	@Override
	public void processPropertiesFile(String propFileName) {
		base.processPropertiesFile(propFileName);
	}

	@Override
	public void setGlobalOption(String name, String value) {
		base.setGlobalOption(name, value);
	}

	@Override
	public String getOutputType() {
		return base.getOutputType();
	}

	@Override
	public void setOutputType(String type) {
		base.setOutputType(type);
	}

	@Override
	public void setHostName(String hostName) {
		base.setHostName(hostName);
	}

	@Override
	public void setPort(String port) {
		base.setPort(port);
	}

	@Override
	public String getPort() {
		return base.getPort();
	}

	@Override
	public void setUserName(String userName) {
		base.setUserName(userName);
	}

	@Override
	public void setUserPassword(String password) {
		base.setUserPassword(password);
	}

	@Override
	public Credentials getCredentials() {
		return base.getCredentials();
	}

	@Override
	public void setCredentials(Credentials credentials) {
		base.setCredentials(credentials);
	}

	@Override
	public String getHostName() {
		return base.getHostName();
	}

	@Override
	public void setDomain(String domain) {
		base.setDomain(domain);
	}

	@Override
	public String getDomain() {
		return base.getDomain();
	}

	@Override
	public void setVerbose(String verboseOutput) {
		base.setVerbose(verboseOutput);
	}

	@Override
	public void setDebug(String debugOutput) {
		base.setDebug(debugOutput);
	}

	@Override
	public void setFailOnError(boolean failOnError) {
		base.setFailOnError(failOnError);
	}

	@Override
	public void setRollbackOnError(boolean enableRollback) {
		base.setRollbackOnError(enableRollback);
	}

	@Override
	public void removeCheckpoint() {
		base.removeCheckpoint();
	}

	@Override
	public DPDirectBase.Operation createOperation() {
		return (DPDirectBase.Operation) base.createOperation();
	}

	@Override
	public DPDirectBase.Operation createOperation(String operationName) {
		return base.createOperation(operationName);
	}

	@Override
	public void addOperation(String operationName) {
		base.addOperation(operationName);
	}

	@Override
	public String generateXMLInstance(DPDirectBase.Operation operation) {
		return base.generateXMLInstance(operation);
	}

	@Override
	public void postOperationXML() {
		base.postOperationXML();
	}

	@Override
	public String parseResponseMsg(DPDirectBase.Operation operation, boolean handleError) {
		return base.parseResponseMsg(operation, handleError);
	}

	@Override
	public boolean isSuccessResponse(DPDirectBase.Operation operation) {
		return base.isSuccessResponse(operation);
	}

	@Override
	public String processResponse(DPDirectBase.Operation operation) {
		return base.processResponse(operation);
	}

	@Override
	public String generateAndPost(DPDirectBase.Operation operation) {
		return base.generateAndPost(operation);
	}

	@Override
	public String postXMLInstance(DPDirectBase.Operation operation, Credentials credentials) {
		return base.postXMLInstance(operation, credentials);
	}

	@Override
	public Credentials getCredentialsFromNetrcConfig(String hostName) {
		return base.getCredentialsFromNetrcConfig(hostName);
	}

	@Override
	public void setFirmware(String firmwareLevel) {
		base.setFirmware(firmwareLevel);
	}

	@Override
	public String getNetrcFilePath() {
		return base.getNetrcFilePath();
	}

	@Override
	public void setNetrcFilePath(String netrcFilePath) {
		base.setNetrcFilePath(netrcFilePath);
	}

}

