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

	private final DPDirectBase base = new DPDirectTaskBase();

	private static class DPDirectTaskBase extends DPDirectBase {
		//@Override
		//public String processResponse(Operation operation) {
		//	System.out.print("highjacking processResponse");
		//	return super.processResponse(operation);
		//}
	}

	public DPDirectTask() {}

	@Override
	public void execute() throws BuildException {
		System.out.println(getProject());
		base.execute();
	}

	@Override
	public void setHostName(String hostName) {
		base.setHostName(hostName);
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
	public void setPort(String port) {
		base.setPort(port);
	}

	@Override
	public void setDomain(String domain) {
		base.setDomain(domain);
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
	public void setOutputType(String type) {
		base.setOutputType(type);
	}

	@Override
	public void setVerbose(String verboseOutput) {
		base.setVerbose(verboseOutput);
	}

	@Override
	public void setFirmware(String firmwareLevel) {
		base.setFirmware(firmwareLevel);
	}

	@Override
	public void setSchema(String schema) {
		base.setSchema(schema);
	}

	@Override
	public Operation createOperation() {
		return base.createOperation();
	}
}

