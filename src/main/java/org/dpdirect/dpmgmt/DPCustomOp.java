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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.dpdirect.utils.FileUtils;

import static org.dpdirect.dpmgmt.Defaults.DEFAULT_POLL_INT_MILLIS;
import static org.dpdirect.dpmgmt.Defaults.DEFAULT_WAIT_TIME_SECONDS;

/**
 * Class for the management of IBM DataPower device via the XML management
 * interface.
 *
 * @author Tim Goodwill
 */
public class DPCustomOp {

	/** The name of the custom operation. */
	protected String customOpName;

	/** The regular operation name underlying the custom op. */
	protected String baseOpName;

	/** The operation instance representing this custom op */
	protected Operation op = null;

	/** The base instance of DPDirectBase. */
	protected DPDirectBase DPDBase = null;

	/** The object name. */
	protected String objectName = null;

	/** The object class name. */
	protected String objectClass = null;

	/** List of currently attached monitors. */
	protected List<String> monitorList = new ArrayList<String>();

	/** Has this custom operation been configured. */
	protected boolean configured = false;

	/** Has this custom operation been logged. */
	protected boolean postLogged = false;

	/** Does this custom operation poll the device. */
	protected boolean polls = false;

	/** Number of log initial lines to display. */
	protected int tailLines = 0;

	/** list of displayed log lines when tailing. */
	protected List<String> lineList = new ArrayList<String>();

	/** Default number of tail log lines. */
	public static int DEFAULT_TAIL_LINES_COUNT = 12;

	/**
	 * @param tailLogLines
	 *            the tailLogLines to set
	 */
	public static void setDefaultTailLines(int tailLogLines) {
		DEFAULT_TAIL_LINES_COUNT = tailLogLines;
	}

	/**
	 * @returns default tailLogLines
	 */
	public static int getDefaultTailLines() {
		return DEFAULT_TAIL_LINES_COUNT;
	}

	/**
	 * The log poll interval as a value in milliseconds.
	 */
	protected int pollIntMillis = DEFAULT_POLL_INT_MILLIS;

	/**
	 * The tail-log poll interval as a value in milliseconds.
	 */
	public static int LOG_POLL_INT_MILLIS = 500;

	/**
	 * The tail-count (message-counts) poll interval as a value in milliseconds.
	 */
	public static int COUNT_POLL_INT_MILLIS = DEFAULT_WAIT_TIME_SECONDS;

	/**
	 * DataPower message codes that will be omitted in tail-log operations.
	 */
	public static String[] OMITED_DP_MSG_IDS = {
			Constants.LOGIN_DP_MSG_ID,
			Constants.LOGOUT_DP_MSG_ID,
			Constants.SESSION_CLOSED_DP_MSG_ID
	};
	/**
	 * HashMap of all custom operations configured for this class,
	 * and corresponding underlying valid SOMA or AMP operation name.
	 */
	public static HashMap<String,String> customOps = new HashMap<String,String>(){{
		put(Constants.SET_DIR_CUSTOM_OP_NAME, Constants.SET_FILE_OP_NAME);
		put(Constants.GET_DIR_CUSTOM_OP_NAME, Constants.GET_FILE_OP_NAME);
		put(Constants.SET_FILES_CUSTOM_OP_NAME, Constants.DO_IMPORT_OP_NAME);
		put(Constants.GET_FILES_CUSTOM_OP_NAME, Constants.DO_EXPORT_OP_NAME);
		put(Constants.TAIL_LOG_CUSTOM_OP_NAME, Constants.GET_FILE_OP_NAME);
		put(Constants.TAIL_COUNT_CUSTOM_OP_NAME, Constants.GET_STATUS_OP_NAME);
	}};

	/**
	 * @return does the name correspond to a custom operation?
	 */
	public static final boolean isCustomOperation(String name){
		return customOps.keySet().contains(name);
	}


	/**
	 * Default constructor for nested Operation class.
	 */
	public DPCustomOp() {
	}

	/**
	 * Named Constructor for for nested Operation class.
	 *
	 * @param operationName
	 *            String : the name of this operation.
	 */
	public DPCustomOp(Operation operation, String customOpName) {
		this.op = operation;
		this.customOpName = customOpName;
		this.baseOpName = customOps.get(this.customOpName);
		this.DPDBase = op.getOuterInstance();
		if (!this.configured) {
			configureCustomOperation();
		}
	}

	/**
	 * @return the name of this custom operation.
	 */
	public String getName(){
		return customOpName;
	}

	/**
	 * @return the underlying valid SOMA or AMP operation name.
	 */
	public String getBaseName(){
		return baseOpName;
	}

	/**
	 * Configures custom operations by mapping single operations to a sequence
	 * of SOMA operations as required to achieve the operation goal.
	 */
	protected boolean configureCustomOperation() {
		synchronized (this.customOpName) {
			if (Constants.TAIL_COUNT_CUSTOM_OP_NAME
					.equals(customOpName)) {
				if (null != objectName && null != objectClass) {
					configured = true;
					this.polls = true;
					setPollIntMillis(LOG_POLL_INT_MILLIS);
					if (this.getTailLogLines() == 0) {
						this.setTailLogLines(getDefaultTailLines());
					}
					createMonitor();
				}
				//Message Counter requires valid class and name attributes (eg.'class=MultiProtocolGateway name=MyGateway')
			} else if (Constants.TAIL_LOG_CUSTOM_OP_NAME
					.equals(customOpName)) {
				configured = true;
				this.polls = true;
				if (this.getTailLogLines() == 0) {
					this.setTailLogLines(getDefaultTailLines());
				}
			} else if (Constants.SET_DIR_CUSTOM_OP_NAME
					.equals(customOpName)) {
				if (null != op.getSrcDir() && null != op.getDestDir()) {
					configured = true;
					multipleSetFile();
				}
			} else if (Constants.GET_DIR_CUSTOM_OP_NAME
					.equals(customOpName)) {
				if (null != op.getSrcDir() && null != op.getDestDir()) {
					configured = true;
					multipleGetFile();
				}
			} else if (Constants.SET_FILES_CUSTOM_OP_NAME
					.equals(customOpName)) {
				if (null != op.getSrcDir() && null != op.getDestDir()) {
					configured = true;
					multipleSetFile();
				}
			} else if (Constants.GET_FILES_CUSTOM_OP_NAME
					.equals(customOpName)) {
				if (null != op.getSrcDir() && null != op.getDestDir()) {
					configured = true;
					getFilesViaDoExport();
				}
			}
		}
		return configured;
	}

	/**
	 * A custom op may intercept a regular post to the device
	 * if a custom approach is required.
	 *
	 * @return intercept the post.
	 */
	public boolean customPostIntercept() {
		boolean interceptPost = false;
		if (Constants.TAIL_LOG_CUSTOM_OP_NAME.equals(customOpName)) {
			System.out
					.println("Type 'enter' to return to cmd prompt.\n");
			if (null == op.getOptionValue(Constants.NAME_OPT_NAME)) {
				op.addOption(Constants.NAME_OPT_NAME,
						Constants.LOGTEMP_DIR_NAME + ":/"
								+ Constants.DEFAULT_LOG_FILE_NAME);
			}
			DataInputStream dis = new DataInputStream(System.in);
			try {
				while (0 == dis.available()) {
					String responseXML = DPDBase.generateAndPost(op);
					op.response = responseXML;
					processTail(op);
					for (long stop = System.nanoTime()
							+ TimeUnit.MILLISECONDS
									.toNanos(pollIntMillis); stop > System
							.nanoTime();) {
						if (0 != dis.available()) {
							break;
						}
					}
				}
			} catch (IOException ex) {
				if (!DPDBase.getFailOnError() && !DPDBase.getLogger().isDebugEnabled()) {
					DPDBase.getLogger().error(ex.getMessage());
				} else {
					DPDBase.getLogger().error(ex.getMessage(), ex);
				}
			} finally {
				interceptPost = true;
			}
		}
		return interceptPost;
	}

	/**
	 * Utility method to create/modify custom options from name/value pair.
	 *
	 * @param optionName
	 *            the name of the option.
	 * @param optionValue
	 *            the value of the option.
	 */
	public void addCustomOptions(String optionName, String optionValue) {
		// set custom values
		if (Constants.NAME_OPT_NAME.equalsIgnoreCase(optionName)) {
			setObjectName(optionValue);
		}
		else if (Constants.CLASS_OPT_NAME.equalsIgnoreCase(optionName)) {
			setObjectClass(optionValue);
		}
		else if (Constants.LINES_OPT_NAME.equalsIgnoreCase(optionName)) {
			int tailLogLines = getDefaultTailLines();
			try {
				tailLogLines = Integer.parseInt(optionValue);
			} catch (NumberFormatException ex) {
				// Ignore.
			}
			setTailLogLines(tailLogLines);
		}

		// alter option values where appropriate
		if (Constants.TAIL_COUNT_CUSTOM_OP_NAME.equals(customOpName)) {
			if (Constants.CLASS_OPT_NAME.equalsIgnoreCase(optionName)
					&& !(optionValue == Constants.MESSAGE_COUNTS_OPT_VALUE)) {
				optionValue = Constants.MESSAGE_COUNTS_OPT_VALUE;
				op.getOption(Constants.CLASS_OPT_NAME).setValue(optionValue);
			}
		}
		else if (Constants.TAIL_LOG_CUSTOM_OP_NAME.equals(customOpName)) {
			if (Constants.NAME_OPT_NAME.equalsIgnoreCase(optionName)
				   && (!optionValue.contains(Constants.LOGTEMP_DIR_NAME + ":"))) {
				optionValue = Constants.LOGTEMP_DIR_NAME + ":/" + optionValue.trim();
				op.getOption(Constants.NAME_OPT_NAME).setValue(optionValue);
			}
		}

		// configure against set values
		if (!this.configured) {
			configureCustomOperation();
		}
	}

	/**
	 * @return the operation polls
	 */
	public boolean isPolling() {
		return polls;
	}

	/**
	 * @return the logList
	 */
	public List<String> getLogList() {
		return lineList;
	}

	/**
	 * Clears the log list.
	 *
	 * @param lineList
	 *            the logList to set
	 */
	public void resetLogList() {
		this.lineList = new ArrayList<String>();
	}

	/**
	 * @return the tailLogLines
	 */
	public int getTailLogLines() {
		return tailLines;
	}

	/**
	 * @param tailLogLines
	 *            the tailLogLines to set
	 */
	public void setTailLogLines(int tailLogLines) {
		this.tailLines = tailLogLines;
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
	 * @return the object name for use with custom operations
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * Set the objectName for use with custom operations
	 */
	public void setObjectName(String objectName) {
		if (null == this.getObjectName()) {
			this.objectName = objectName;
		}
	}

	/**
	 * Get the post-logged flag for logging purposes
	 */
	public boolean getPostLogged() {
		return this.postLogged;
	}

	/**
	 * Set the post-logged flag for logging purposes
	 */
	public void setPostLogged(boolean logged) {
		this.postLogged = logged;
	}

	/**
	 * @return the object class for use with custom operations
	 */
	public String getObjectClass() {
		return this.objectClass;
	}

	/**
	 * Set the object class for use with custom operations
	 */
	public void setObjectClass(String objectClass) {
		if (null == this.getObjectClass()) {
			this.objectClass = objectClass;
		}
	}

	/**
	 * Get a consistent string representing a temporary monitor name
	 */
	public String getTempMonitorName() {
		return DPDBase.getCredentials().getUserName() + "-" + this.getObjectName()
				+ "-" + customOpName;
	}

	/**
	 * Filter lines using supplied filter.
	 *
	 * @param operation
	 *            Operation : the current operation object.
	 * @param parsedText
	 *            String : the parsed output lines.
	 */
	public StringBuffer appendLines(Operation operation, String parsedText) {
		boolean uniqueEntries = (Constants.TAIL_LOG_CUSTOM_OP_NAME
				.equalsIgnoreCase(customOpName));
		List<String> lineList = getLogList();
		int displayLines = getTailLogLines();

		StringBuffer outputLines = new StringBuffer();

		String[] lines = parsedText.split("(?=\\d{8}T\\d{6}\\.\\d+Z\\s+\\[0x)");

		if (lineList.isEmpty()) {
			int linesPrinted = 0;
            for (String line : lines) {
                lineList.add(line);
                if (line.length() > 1) {
                    if (displayLines > 0) {
                        if (++linesPrinted < displayLines) {
                            outputLines.append(line).append("\n");
                        }
                    } else {
                        outputLines.append(line).append("\n");
                    }
                }
            }
		} else {
			outer: for (String line : lines) {
				// filter out specific message codes related to auth
				inner: for (String messageId : OMITED_DP_MSG_IDS) {
					if (line.contains("[" + messageId + "]")) {
						continue outer;
					}
				}

                if (!lineList.contains(line) || !uniqueEntries) {
					lineList.add(line);

					line = line.replaceAll("(?i)(error)", Constants.ANSI_RED + "$1" + Constants.ANSI_RESET);
					line = line.replaceAll("(?i)(warn)", Constants.ANSI_ORANGE + "$1" + Constants.ANSI_RESET);

					/* print out to console */
					if (line.length() > 1) {
						outputLines.append(line).append("\n");
					}
                }
            }
		}
		return outputLines;
	}

	/**
	 * Decode and parse the file contents and return only new lines.
	 *
	 * @param operation
	 *            Operation : the current operation object.
	 */
	public boolean processTail(Operation operation) {
		boolean success = true;
		StringBuffer outputLines = new StringBuffer();
		String parsedText = null;

		parsedText = DPDBase.parseResponseMsg(operation, false);
		DPDBase.getLogger().debug("parsedText=" + parsedText);

		if (null == parsedText) {
			success = false;
		} else {
			outputLines = appendLines(operation, parsedText);

			if (0 < outputLines.length()) {
				System.out.println(outputLines.toString().trim());
			}
		}

		return success;
	}

	/**
	 * Recurse source directory and create multiple set-file operations to
	 * upload.
	 */
	protected void multipleSetFile() {
		File sourceDirectory = new File(op.getSrcDir());
		try {
			List<File> fileList = FileUtils
					.getFilesFromDirectory(sourceDirectory);
			Iterator<File> i = fileList.iterator();
			if (i.hasNext()) {
				if (op.isReplace() && op.isOverwrite()) {
					Operation removeDirOp = DPDBase.newOperation(
							Constants.REMOVE_DIR_OP_NAME);
					removeDirOp.setSuppressResponse(true);
					removeDirOp.setParentOperation(this);
					// must precede current operation
					op.addToOperationChain(op.getOperationChain().indexOf(op),
							removeDirOp);
					removeDirOp.addOption(Constants.REMOVE_DIR_OP_NAME + "."
							+ Constants.DIR_OP_NAME, op.getDestDir());
			    }
				Operation createDirOp = DPDBase.newOperation(
						Constants.CREATE_DIR_OP_NAME);
				createDirOp.setSuppressResponse(true);
				createDirOp.setParentOperation(this);
				// must precede current operation
				op.addToOperationChain(op.getOperationChain().indexOf(op),
						createDirOp);
				createDirOp.addOption(Constants.CREATE_DIR_OP_NAME
						+ "." + Constants.DIR_OP_NAME, op.getDestDir());
			}
			while (i.hasNext()) {
				File file = (File) i.next();
				String relativePath = file.getAbsolutePath()
						.replace("\\", "/").replaceAll(op.getSrcDir(), "");
				if (file.isDirectory()) {
					Operation createDirOp = DPDBase.newOperation(
							Constants.CREATE_DIR_OP_NAME);
					createDirOp.setSuppressResponse(true);
					createDirOp.setParentOperation(this);
					// must precede current operation
					op.addToOperationChain(op.getOperationChain().indexOf(op),
							createDirOp);
					createDirOp.addOption(Constants.CREATE_DIR_OP_NAME
							+ "." + Constants.DIR_OP_NAME, op.getDestDir()
							+ relativePath);
				} else {
					if (null != op.getEndPoint() && op.getEndPoint()
							.equals(Constants.SOMA_MGMT_2004_URL)) {
						op.addOption(Constants.SET_FILE_OP_NAME + "@"
								+ Constants.NAME_OPT_NAME, op.getDestDir()
								+ relativePath);
						op.addOption(Constants.SET_FILE_OP_NAME, file);
					} else if (null == op.getSrcFile()) {
						if (i.hasNext()) {
							op.setSuppressResponse(true);
						}
						op.addOption(Constants.SET_FILE_OP_NAME
								+ "@" + Constants.NAME_OPT_NAME, op.getDestDir()
								+ relativePath);
						op.addOption(Constants.SET_FILE_OP_NAME, file);
					} else {
						Operation setFileOp = DPDBase.createOperation(Constants.SET_FILE_OP_NAME);
						setFileOp.setMemSafe(op.getMemSafe());
						setFileOp.setOverwrite(op.getOverwrite());
						setFileOp.setParentOperation(this);
						if (i.hasNext()) {
							setFileOp.setSuppressResponse(true);
						}
						setFileOp.addOption(Constants.SET_FILE_OP_NAME
								+ "@" + Constants.NAME_OPT_NAME, op.getDestDir()
								+ relativePath);
						setFileOp.addOption(Constants.SET_FILE_OP_NAME,
								file);
					}
				}
			}
		} catch (IOException ex) {
			if (!DPDBase.getFailOnError() && !DPDBase.getLogger().isDebugEnabled()) {
				DPDBase.getLogger().error(ex.getMessage());
			} else {
				DPDBase.getLogger().error(ex.getMessage(), ex);
			}
		}
	}
	
	/**
	 * Zip source directory and create do-import operation to
	 * upload.
	 * This method is an historical artifact. The procedure is unbelievably slow.
	 */
	protected void setFilesViaDoImport() {
		File sourceDirectory = new File(op.getSrcDir());
		String zipDirPath = op.getDestDir().replace("://", "");
		try {
			String zipFilePath = sourceDirectory.getCanonicalPath() + ".zip";
			FileUtils.zipDirectoryForImport(zipDirPath, sourceDirectory, zipFilePath, op.getEffectiveDomain());
			op.setSrcFile(zipFilePath);
			op.addOption("overwrite-files", "true");
			op.addOption("source-type", "ZIP");
		} catch (Exception ex) {
			if (!DPDBase.failOnError && !DPDBase.getLogger().isDebugEnabled()) {
				DPDBase.getLogger().error(ex.getMessage());
			} else {
				DPDBase.getLogger().error(ex.getMessage(), ex);
			}
		}
	}
	
	/**
	 * Delete set-files temporary zip file
	 */
	protected void deleteTempZipFile() {
		if (op.getSrcFile().substring(op.getSrcFile().length()-3).equalsIgnoreCase("zip")) {
			File tempZipFile = new File(op.getSrcFile());
			tempZipFile.delete();
		}
	}

	/**
	 * Recurse DP source directory via 'get-filestore' operation and create
	 * multiple get-file operations to download.
	 */
	protected void multipleGetFile() {
		op.setSuppressResponse(true);
		List<String> filePaths = new ArrayList<String>();
		Operation getFilestoreOp = DPDBase.newOperation(
				Constants.GET_FILESTORE_OP_NAME);
		getFilestoreOp.addOption(Constants.GET_FILESTORE_OP_NAME + "@"
				+ Constants.LOCATION_OPT_NAME, Constants.LOCAL_DIR_NAME
				+ ":");
		getFilestoreOp
				.addOption(Constants.GET_FILESTORE_OP_NAME + "@"
						+ Constants.LAYOUT_ONLY_OPT_NAME,
						Constants.FALSE_OPT_VALUE);
		getFilestoreOp
				.addOption(Constants.GET_FILESTORE_OP_NAME + "@"
						+ Constants.ANNOTATED_OPT_NAME,
						Constants.FALSE_OPT_VALUE);
		DPDBase.setSchema();
		
		DPDBase.generateXMLInstance(getFilestoreOp);
		getFilestoreOp.setResponse(DPDBase.postXMLInstance(getFilestoreOp, DPDBase.getCredentials()));
//		DPDBase.parseResponseMsg(getFilestoreOp, false);
		try {
			filePaths = getFilestoreOp.getResponseParser().parseGetFileset(getFilestoreOp.getResponse());
			if (DPDBase.getLogger().isDebugEnabled()) {
				DPDBase.getLogger().debug("File paths: " + filePaths);
			}
		} catch (Exception e) {
			DPDBase.getLogger().debug("Error: could not parse file paths.");
			// do nothing
		}
		
		String srcPath = op.srcDir.replace("///", "/");
		if (DPDBase.getLogger().isDebugEnabled()) {
			DPDBase.getLogger().debug("Source path: " + srcPath);
		}
			
		for (String dpPath : filePaths) {
			if (dpPath.contains(srcPath + "/")) {
				if (DPDBase.getLogger().isDebugEnabled()) {
					DPDBase.getLogger().debug("File path: " + dpPath);
				}
				String relativePath = dpPath.replaceAll(srcPath, "");
				String destPath = op.destDir + relativePath;
				if (null != op.destFile) {
					op.addOption(Constants.GET_FILE_OP_NAME + "@"
							+ Constants.NAME_OPT_NAME, dpPath);
					op.destFile = destPath;
				} else {
					Operation getFile = DPDBase.createOperation(Constants.GET_FILE_OP_NAME);
					getFile.setMemSafe(op.getMemSafe());
					getFile.setOverwrite(op.getOverwrite());
					getFile.addOption(Constants.GET_FILE_OP_NAME + "@"
							+ Constants.NAME_OPT_NAME, dpPath);
					getFile.destFile = destPath;
				}
			}
		}
	}


	/**
	 * Retrieve directory from device via 'do-export' operation, Result is
	 * unzipped and saved to nominated directory.
	 */
	protected void getFilesViaDoExport() {
		if (null != op.getSrcDir() && null != op.getDestDir()
				&& null != op.getName()) {
			op.addOption(Constants.DO_EXPORT_OP_NAME + "@"
					+ Constants.FORMAT_OPT_NAME, Constants.ZIP_OPT_VALUE);
			op.addOption(Constants.DO_EXPORT_OP_NAME + "@"
					+ Constants.ALL_FILES_OPT_NAME,
					Constants.TRUE_OPT_VALUE);
		}
	}

	protected void createMonitor() {
		String objectName = this.getObjectName();
		String tempMonitorName = getTempMonitorName();
		setPollIntMillis(COUNT_POLL_INT_MILLIS);
		op.setFilter(tempMonitorName);
		setMonitorList();
		createMonitorObjects(tempMonitorName);
		List<String> currentMonitorList = new ArrayList<String>(
				this.monitorList);
		currentMonitorList.add(tempMonitorName);
		modifyMonitorConfig(currentMonitorList);
	}

	protected boolean removeMonitor() {
		String tempMonitorName = getTempMonitorName();
		modifyMonitorConfig(this.monitorList);
		return deleteMonitorObjects(tempMonitorName);
	}

	protected boolean setMonitorList() {
		boolean success = false;
		Operation getStatusOp = DPDBase.newOperation(Constants.GET_CONFIG_OP_NAME);
		getStatusOp.addOption(Constants.NAME_OPT_NAME, this.getObjectName());
		getStatusOp.addOption(Constants.CLASS_OPT_NAME, this.getObjectClass());
		DPDBase.generateXMLInstance(getStatusOp);
		String xmlResponse = DPDBase.postXMLInstance(getStatusOp, DPDBase.getCredentials());
		getStatusOp.setResponse(xmlResponse);
		success = DPDBase.isSuccessResponse(getStatusOp);
		if (success) {
			this.monitorList = getStatusOp.getResponseParser()
					.getContent(Constants.COUNT_MONITORS_OPT_VALUE);
		}
		return success;
	}

	protected void modifyMonitorConfig(List<String> monitorList) {
		Operation modifyConfigOp = DPDBase.newOperation(
				Constants.MODIFY_CONFIG_OP_NAME);
		modifyConfigOp.addOption(this.getObjectClass() + "@"
				+ Constants.NAME_OPT_NAME, this.getObjectName());

		if (!monitorList.isEmpty()) {
			for (String existingMonitorName : monitorList) {
				modifyConfigOp.addOption(Constants.COUNT_MONITORS_OPT_VALUE,
						existingMonitorName);
			}
		} else {
			modifyConfigOp.addOption(Constants.COUNT_MONITORS_OPT_VALUE);
		}

		DPDBase.generateXMLInstance(modifyConfigOp);
		String xmlResponse = DPDBase.postXMLInstance(modifyConfigOp, DPDBase.getCredentials());
		modifyConfigOp.setResponse(xmlResponse);
		DPDBase.parseResponseMsg(modifyConfigOp, true);
	}

	/**
	 * Set up Message Count Monitor Objects
	 */
	protected void createMonitorObjects(String tempMonitorName) {
		List<Operation> tempOpList = new ArrayList();

		Operation messageMatchingOp = DPDBase.newOperation(
				Constants.SET_CONFIG_OP_NAME);
		messageMatchingOp.addOption(Constants.MESSAGE_MATCHING_OPT_NAME + "@"
				+ Constants.NAME_OPT_NAME, tempMonitorName);
		messageMatchingOp.addOption(Constants.MESSAGE_MATCHING_OPT_NAME + "."
				+ Constants.HTTP_METHOD_OPT_NAME, Constants.ANY_OPT_VALUE);
		messageMatchingOp.addOption(Constants.MESSAGE_MATCHING_OPT_NAME + "."
				+ Constants.REQUEST_URL_OPT_NAME, Constants.WILDCARD_OPT_VALUE);
		tempOpList.add(messageMatchingOp);

		Operation messageTypeOp = DPDBase.newOperation(Constants.SET_CONFIG_OP_NAME);
		messageTypeOp.addOption(Constants.MESSAGE_TYPE_OPT_NAME + "@"
				+ Constants.NAME_OPT_NAME, tempMonitorName);
		messageTypeOp.addOption(Constants.MESSAGE_TYPE_OPT_NAME + "."
				+ Constants.MATCHING_OPT_NAME, tempMonitorName);
		tempOpList.add(messageTypeOp);

		Operation createFilterActionOp = DPDBase.newOperation(
				Constants.SET_CONFIG_OP_NAME);
		createFilterActionOp.addOption(Constants.FILTER_ACTION_OPT_NAME + "@"
				+ Constants.NAME_OPT_NAME, tempMonitorName);
		createFilterActionOp.addOption(Constants.FILTER_ACTION_OPT_NAME + "."
				+ Constants.TYPE_OPT_NAME, Constants.NOTIFY_OPT_VALUE);
		createFilterActionOp.addOption(Constants.FILTER_ACTION_OPT_NAME + "."
				+ Constants.LOG_LEVEL_OPT_VALUE, Constants.DEBUG_OPT_VALUE);
		tempOpList.add(createFilterActionOp);

		Operation countMonitorOp = DPDBase.newOperation(Constants.SET_CONFIG_OP_NAME);
		countMonitorOp.addOption(Constants.COUNT_MONITOR_OPT_NAME + "@"
				+ Constants.NAME_OPT_NAME, tempMonitorName);
		countMonitorOp.addOption(Constants.COUNT_MONITOR_OPT_NAME + "."
				+ Constants.MESSAGE_TYPE_OPT_NAME, tempMonitorName);
		countMonitorOp.addOption(Constants.COUNT_MONITOR_OPT_NAME + "."
				+ Constants.SOURCE_OPT_NAME, Constants.ALL_OPT_VALUE);
		countMonitorOp.addOption(Constants.COUNT_MONITOR_OPT_NAME + "."
				+ Constants.MEASURE_OPT_NAME, Constants.REQUESTS_OPT_VALUE);
		countMonitorOp.addOption("Filter.Name", tempMonitorName);
		countMonitorOp.addOption("Filter.Interval", "1000");
		countMonitorOp.addOption("Filter.RateLimit", "50");
		countMonitorOp.addOption("Filter.BurstLimit", "100");
		countMonitorOp.addOption("Filter.Action", tempMonitorName);
		tempOpList.add(countMonitorOp);

		DPDBase.setSchema();

		for (Operation op : tempOpList) {
			DPDBase.generateXMLInstance(op);
			String xmlResponse = DPDBase.postXMLInstance(op, DPDBase.getCredentials());
			op.setResponse(xmlResponse);
			DPDBase.parseResponseMsg(op, true);
		}
	}

	/**
	 * Remove Message Count Monitor Objects
	 */
	protected boolean deleteMonitorObjects(String tempMonitorName) {
		boolean success = false;
		Operation delConfigOp = DPDBase.newOperation(Constants.DEL_CONFIG_OP_NAME);
		delConfigOp.addOption(Constants.COUNT_MONITOR_OPT_NAME + "@"
				+ Constants.NAME_OPT_NAME, tempMonitorName);
		delConfigOp.addOption(Constants.MESSAGE_TYPE_OPT_NAME + "@"
				+ Constants.NAME_OPT_NAME, tempMonitorName);
		DPDBase.generateXMLInstance(delConfigOp);
		String xmlResponse = DPDBase.postXMLInstance(delConfigOp, DPDBase.getCredentials());
		delConfigOp.setResponse(xmlResponse);
		success = DPDBase.isSuccessResponse(delConfigOp);

		delConfigOp = DPDBase.newOperation(Constants.DEL_CONFIG_OP_NAME);
		delConfigOp.addOption(Constants.MESSAGE_MATCHING_OPT_NAME + "@"
				+ Constants.NAME_OPT_NAME, tempMonitorName);
		delConfigOp.addOption(Constants.FILTER_ACTION_OPT_NAME + "@"
				+ Constants.NAME_OPT_NAME, tempMonitorName);

		DPDBase.generateXMLInstance(delConfigOp);
		xmlResponse = DPDBase.postXMLInstance(delConfigOp, DPDBase.getCredentials());
		delConfigOp.setResponse(xmlResponse);
		success = DPDBase.isSuccessResponse(delConfigOp);
		return success;
	}

}
