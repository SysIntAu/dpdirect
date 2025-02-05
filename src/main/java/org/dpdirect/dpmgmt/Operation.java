package org.dpdirect.dpmgmt;

import org.apache.log4j.Logger;
import org.dpdirect.schema.SchemaLoader;
import org.dpdirect.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static org.dpdirect.dpmgmt.Defaults.DEFAULT_POLL_INT_MILLIS;
import static org.dpdirect.dpmgmt.Defaults.DEFAULT_WAIT_TIME_SECONDS;

/**
 * Inner class representing nested or stacked individual DP SOMA or AMP
 * operations. Several operations may belong to a single DPDirect session.
 */
public class Operation {

    protected final static Logger log = Logger.getLogger(Operation.class);

    protected DPDirectBase base = null;

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

    protected String waitForXPath = null;

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
    public Operation(DPDirectBase base) {
        this.base = base;
    }

    /**
     * Named Constructor for for nested Operation class.
     *
     * @param operationName
     *            String : the name of this operation.
     */
    public Operation(DPDirectBase base, String operationName) {
        this.base = base;
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
        else if (null != this.waitFor || null != this.waitForXPath){
            base.pollForResult(this);
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
                && Constants.PARSED_OUTPUT_OPT_NAME.equalsIgnoreCase(base.getOutputType())){
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
     * Set the waitForXPath value
     *
     * @param xpath
     *            operation result to poll for.
     */
    public void setWaitForXPath(String xpath) {
        this.waitForXPath = xpath;
    }

    /**
     * Get the waitForXPath value
     *
     * @return the waitForXPath value
     */
    public String getWaitForXPath() {
        return this.waitForXPath;
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
        return base.getDPDInstance();
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
            return base.getDefaultDomain();
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
        responseParser.setOutputType(base.getOutputType());
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