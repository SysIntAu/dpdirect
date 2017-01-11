package org.dpdirect.dpmgmt;

/**
 * A collection of constants for general reuse.
 * 
 * @author Tim Goodwill
 */
public class Constants {

   /**
    * Local resource path of the "ant-usage.txt" usage text file.
    */
   public static final String ANT_USAGE_TEXT_FILE_PATH = "/usage/ant-usage.txt";

   /**
    * Local resource path of the "console-usage.txt" usage text file.
    */
   public static final String CONSOLE_USAGE_TEXT_FILE_PATH = "/usage/console-usage.txt";
   
   /**
    * Local resource path of the "cmd-usage.txt" usage text file.
    */
   public static final String CMD_USAGE_TEXT_FILE_PATH = "/usage/cmd-usage.txt";
   
   /**
    * The help "ant" option name.
    */
   public static final String USAGE_HELP_ANT = "ant";
   
   /**
    * The help "console" option name.
    */
   public static final String USAGE_HELP_CONSOLE = "console";
   
   /**
    * The help "cmdline" option name.
    */
   public static final String USAGE_HELP_CMDLINE = "cmdline";
   
      /**
    * Local resource path of the DataPower SOMA and AMP version 8.0 XML schemas.
    */
   public static final String MGMT_80_SCHEMAS_DIR = "/datapower/8.0/mgmt/";
   
      /**
    * Local resource path of the DataPower SOMA and AMP version 7.0 XML schemas.
    */
   public static final String MGMT_70_SCHEMAS_DIR = "/datapower/7.0/mgmt/";
   
      /**
    * Local resource path of the DataPower SOMA and AMP version 6.0 XML schemas.
    */
   public static final String MGMT_60_SCHEMAS_DIR = "/datapower/6.0/mgmt/";
   
   /**
    * Local resource path of the DataPower SOMA and AMP version 5.0 XML schemas.
    */
   public static final String MGMT_50_SCHEMAS_DIR = "/datapower/5.0/mgmt/";

   /**
    * Local resource path of the DataPower SOMA and AMP version 4.0 XML schemas.
    */
   public static final String MGMT_40_SCHEMAS_DIR = "/datapower/4.0/mgmt/";

   /**
    * Local resource path of the DataPower SOMA and AMP version 4.0 XML schemas.
    */
   public static final String MGMT_30_SCHEMAS_DIR = "/datapower/3.0/mgmt/";
   
   /**
    * Local resource path of the DataPower SOMA 2004 XML schemas.
    */
   public static final String MGMT_2004_SCHEMAS_DIR = "/datapower/2004/mgmt/";
   
   /**
    * Local resource path of the DataPower SOMA 2004 XML schemas.
    */
   public static final String MGMT_DEFAULT_SCHEMAS_DIR = "/datapower/default/mgmt/";

   /**
    * Name of the "xml-mgmt-ops.xsd" SOMA XML Management schema.
    */
   public static final String SOMA_MGMT_SCHEMA_NAME = "xml-mgmt-ops.xsd";

   /**
    * Name of the "app-mgmt-protocol.xsd" AMP XML Management schema.
    */
   public static final String AMP_MGMT_30_SCHEMA_NAME = "app-mgmt-protocol.xsd";

   /**
    * Name of the "app-mgmt-protocol" AMP XML Management schema as of DP Release 4.0.
    */
   public static final String AMP_MGMT_40_SCHEMA_NAME = "app-mgmt-protocol-v2.xsd";
   
   /**
    * Name of the "app-mgmt-protocol" AMP XML Management schema as of DP Release 5.0.
    */
   public static final String AMP_MGMT_50_SCHEMA_NAME = "app-mgmt-protocol-v3.xsd";
   
   /**
    * Name of the "app-mgmt-protocol" AMP XML Management schema as of DP Release 6.0.
    */
   public static final String AMP_MGMT_60_SCHEMA_NAME = "app-mgmt-protocol-v3.xsd";
     
   /**
    * Name of the "app-mgmt-protocol" AMP XML Management schema as of DP Release 7.0.
    */
   public static final String AMP_MGMT_70_SCHEMA_NAME = "app-mgmt-protocol-v3.xsd";
   
   /**
    * Name of the "app-mgmt-protocol" AMP XML Management schema as of DP Release 8.0.
    */
   public static final String AMP_MGMT_80_SCHEMA_NAME = "app-mgmt-protocol-v3.xsd";
   
   /**
    * Name of the Apache licenced "app-mgmt-protocol" AMP XML Management schema.
    */
   public static final String AMP_MGMT_DEFAULT_SCHEMA_NAME = "app-mgmt-protocol-v3.xsd";

   /**
    * Name of the "app-mgmt-protocol.xsd" AMP XML Management schema.
    */
   public static final String SOMA_MGMT_2004_SCHEMA_NAME = "xml-mgmt-2004.xsd";
   
   /**
    * Local resource path of the DataPower SOMA 4.0.0 XML schema.
    */
   public static final String SOMA_MGMT_30_SCHEMA_PATH = MGMT_30_SCHEMAS_DIR + SOMA_MGMT_SCHEMA_NAME;
    
   /**
    * Local resource path of the DataPower SOMA 4.0.0 XML schema.
    */
   public static final String SOMA_MGMT_40_SCHEMA_PATH = MGMT_40_SCHEMAS_DIR + SOMA_MGMT_SCHEMA_NAME;
   
   /**
    * Local resource path of the DataPower SOMA 5.0.0 XML schema.
    */
   public static final String SOMA_MGMT_50_SCHEMA_PATH = MGMT_50_SCHEMAS_DIR + SOMA_MGMT_SCHEMA_NAME;
  
   /**
    * Local resource path of the DataPower SOMA 6.0.0 XML schema.
    */
   public static final String SOMA_MGMT_60_SCHEMA_PATH = MGMT_60_SCHEMAS_DIR + SOMA_MGMT_SCHEMA_NAME;
  
   /**
    * Local resource path of the DataPower SOMA 7.0.0 XML schema.
    */
   public static final String SOMA_MGMT_70_SCHEMA_PATH = MGMT_70_SCHEMAS_DIR + SOMA_MGMT_SCHEMA_NAME;
  
   /**
    * Local resource path of the DataPower SOMA 8.0.0 XML schema.
    */
   public static final String SOMA_MGMT_80_SCHEMA_PATH = MGMT_80_SCHEMAS_DIR + SOMA_MGMT_SCHEMA_NAME;

   /**
    * Local resource path of the DataPower AMP 4.0.0 XML schema.
    */
   public static final String AMP_MGMT_30_SCHEMA_PATH = MGMT_30_SCHEMAS_DIR + AMP_MGMT_30_SCHEMA_NAME;
   
   /**
    * Local resource path of the DataPower AMP 4.0.0 XML schema.
    */
   public static final String AMP_MGMT_40_SCHEMA_PATH = MGMT_40_SCHEMAS_DIR + AMP_MGMT_40_SCHEMA_NAME;
 
   /**
    * Local resource path of the DataPower AMP 5.0.0 XML schema.
    */
   public static final String AMP_MGMT_50_SCHEMA_PATH = MGMT_50_SCHEMAS_DIR + AMP_MGMT_50_SCHEMA_NAME;
 
   /**
    * Local resource path of the DataPower AMP 6.0.0 XML schema.
    */
   public static final String AMP_MGMT_60_SCHEMA_PATH = MGMT_60_SCHEMAS_DIR + AMP_MGMT_60_SCHEMA_NAME;
   
   /**
    * Local resource path of the DataPower AMP 7.0.0 XML schema.
    */
   public static final String AMP_MGMT_70_SCHEMA_PATH = MGMT_70_SCHEMAS_DIR + AMP_MGMT_70_SCHEMA_NAME;
   
   /**
    * Local resource path of the DataPower AMP 8.0.0 XML schema.
    */
   public static final String AMP_MGMT_80_SCHEMA_PATH = MGMT_80_SCHEMAS_DIR + AMP_MGMT_80_SCHEMA_NAME;
     
   /**
    * Local resource path of the Apache licenced DataPower AMP XML schema.
    */
   public static final String AMP_MGMT_DEFAULT_SCHEMA_PATH = MGMT_DEFAULT_SCHEMAS_DIR + AMP_MGMT_DEFAULT_SCHEMA_NAME;
 
   /**
    * Local resource path of the SOMA 2004 XML schema.
    */
   public static final String SOMA_MGMT_2004_SCHEMA_PATH = MGMT_2004_SCHEMAS_DIR + SOMA_MGMT_2004_SCHEMA_NAME;
   
   /**
    * The relative service endpoint URL for the "SOMA" management API endpoint.
    */
   public static final String SOMA_MGMT_CURRENT_URL = "/service/mgmt/current";

   /**
    * Short form for the "SOMA" Schema and/or API endpoint.
    */
   public static final String SOMA_MGMT_SHORT = "SOMA";
   
   /**
    * The relative service endpoint URL for the "SOMA 2004" management API endpoint.
    */
   public static final String SOMA_MGMT_2004_URL = "/service/mgmt/2004";

   /**
    * Short form for the "2004" Schema and/or API endpoint.
    */
   public static final String SOMA_MGMT_2004_SHORT = "2004";
   
   /**
    * The relative service endpoint URL for the "AMP" 3.0.0 management API endpoint.
    */
   public static final String AMP_MGMT_30_URL = "/service/mgmt/amp/1.0";
   
   /**
    * The relative service endpoint URL for the "AMP" 4.0 management API endpoint.
    */
   public static final String AMP_MGMT_40_URL = "/service/mgmt/amp/2.0";
   
   /**
    * The relative service endpoint URL for the "AMP" 5.0 management API endpoint.
    */
   public static final String AMP_MGMT_50_URL = "/service/mgmt/amp/3.0";
  
   /**
    * The relative service endpoint URL for the "AMP" 6.0 management API endpoint.
    */
   public static final String AMP_MGMT_60_URL = "/service/mgmt/amp/3.0";
  
   /**
    * The relative service endpoint URL for the "AMP" 7.0 management API endpoint.
    */
   public static final String AMP_MGMT_70_URL = "/service/mgmt/amp/3.0";
  
   /**
    * The relative service endpoint URL for the "AMP" 8.0 management API endpoint.
    */
   public static final String AMP_MGMT_80_URL = "/service/mgmt/amp/3.0";
 
   /**
    * The relative service endpoint URL for the Apache licenced "AMP" management API endpoint.
    */
   public static final String AMP_MGMT_DEFAULT_URL = "/service/mgmt/amp/3.0";

   /**
    * Short form for the "AMP" Schema and/or API endpoint.
    */
   public static final String AMP_MGMT_SHORT = "AMP";
   
   /**
    * The "domain" option name.
    */
   public static final String DOMAIN_OPT_NAME = "domain";

   /**
    * The "Domain" option name.
    */
   public static final String DOMAIN_UCC_OPT_NAME = "Domain";

   /**
    * The "port" option name.
    */
   public static final String PORT_OPT_NAME = "port";

   /**
    * The "name" option name.
    */
   public static final String NAME_OPT_NAME = "name";

   /**
    * The "class" option name.
    */
   public static final String CLASS_OPT_NAME = "class";

   /**
    * The "format" option name.
    */
   public static final String FORMAT_OPT_NAME = "format";

   /**
    * The "all-files" option name.
    */
   public static final String ALL_FILES_OPT_NAME = "all-files";

   /**
    * The "layout-only" option name.
    */
   public static final String LAYOUT_ONLY_OPT_NAME = "layout-only";
   
   /**
    * The "annotated" option name.
    */
   public static final String ANNOTATED_OPT_NAME = "annotated";

   /**
    * The "location" option name.
    */
   public static final String LOCATION_OPT_NAME = "location";

   /**
    * The "hostName" option name.
    */
   public static final String HOST_NAME_OPT_NAME = "hostName";

   /**
    * The "userName" option name.
    */
   public static final String USER_NAME_OPT_NAME = "userName";

   /**
    * The "userPassword" option name.
    */
   public static final String USER_PASSWORD_OPT_NAME = "userPassword";

   /**
    * The "schema" option name.
    */
   public static final String SCHEMA_OPT_NAME = "schema";

   /**
    * The "firmware" option name.
    */
   public static final String FIRMWARE_OPT_NAME = "firmware";

   /**
    * The "destFile" option name.
    */
   public static final String DEST_FILE_OPT_NAME = "destFile";

   /**
    * The "destDir" option name.
    */
   public static final String DEST_DIR_OPT_NAME = "destDir";

   /**
    * The "srcFile" option name.
    */
   public static final String SRC_FILE_OPT_NAME = "srcFile";

   /**
    * The "srcDir" option name.
    */
   public static final String SRC_DIR_OPT_NAME = "srcDir";

   /**
    * The "endPoint" option name.
    */
   public static final String END_POINT_OPT_NAME = "endPoint";
   
   /**
    * The "Type" option name.
    */
   public static final String TYPE_OPT_NAME = "Type";

   /**
    * The "outputType" option name.
    */
   public static final String OUTPUT_TYPE_OPT_NAME = "outputType";
   
   /**
    * The "Parsed outputType" option name.
    */
   public static final String PARSED_OUTPUT_OPT_NAME = "PARSED";
   
   /**
    * The "debug" option name.
    */
   public static final String DEBUG_OPT_NAME = "debug";
   
   /**
    * The "debug" option value.
    */
   public static final String DEBUG_OPT_VALUE= "debug";

   /**
    * The "all" option value.
    */
   public static final String ALL_OPT_VALUE = "all";

   /**
    * The "verbose" option name.
    */
   public static final String VERBOSE_OPT_NAME = "verbose";
   
   /**
    * The "failOnError" option name.
    */
   public static final String FAIL_ON_ERROR_OPT_NAME = "failOnError";

   /**
    * The "filter" option name.
    */
   public static final String FILTER_OPT_NAME = "filter";

   /**
    * The "filterOut" option name.
    */
   public static final String FILTER_OUT_OPT_NAME = "filterOut";

   /**
    * The "lines" option name.
    */
   public static final String LINES_OPT_NAME = "lines";

   /**
    * The "overwrite" option name.
    */
   public static final String OVERWRITE_OPT_NAME = "overwrite";

   /**
    * The "failState" option name.
    */
   public static final String FAIL_STATE_OPT_NAME = "failState";

   /**
    * The "FilterAction" configuration option name.
    */
   public static final String FILTER_ACTION_OPT_NAME = "FilterAction";

   /**
    * The "CountMonitor" configuration option name.
    */
   public static final String COUNT_MONITOR_OPT_NAME = "CountMonitor";

   /**
    * The "MessageMatching" configuration option name.
    */
   public static final String MESSAGE_MATCHING_OPT_NAME = "MessageMatching";

   /**
    * The "Matching" option name.
    */
   public static final String MATCHING_OPT_NAME = "Matching";

   /**
    * The "Source" option name.
    */
   public static final String SOURCE_OPT_NAME = "Source";

   /**
    * The "MessageType" option name.
    */
   public static final String MESSAGE_TYPE_OPT_NAME = "MessageType";

   /**
    * The "Measure" option name.
    */
   public static final String MEASURE_OPT_NAME = "Measure";
   
   /**
    * The "HTTPMethod" option name.
    */
   public static final String HTTP_METHOD_OPT_NAME = "HTTPMethod";
   
   /**
    * The "RequestURL" option name.
    */
   public static final String REQUEST_URL_OPT_NAME = "RequestURL";
   
   /**
    * The "true" option value.
    */
   public static final String TRUE_OPT_VALUE = "true";

   /**
    * The "false" option value.
    */
   public static final String FALSE_OPT_VALUE = "false";

   /**
    * The "none" option value.
    */
   public static final String NONE_OPT_VALUE = "none";

   /**
    * The "ZIP" option value.
    */
   public static final String ZIP_OPT_VALUE = "ZIP";

   /**
    * The "0x00000000" event code option value.
    */
   public static final String EXPECTED_STATUS_RESPONSE = "0x00000000"; 
   									//"0x00000000|AdminState: disabled";
   /**
    * The "ObjectStatus" option value.
    */
   public static final String OBJECT_STATUS_OPT_VALUE = "ObjectStatus";

   /**
    * The "MessageCounts" option value.
    */
   public static final String MESSAGE_COUNTS_OPT_VALUE = "MessageCounts";
   
   /**
    * The "notify" option value.
    */
   public static final String NOTIFY_OPT_VALUE = "notify";
   
   /**
    * The "any" option value.
    */
   public static final String ANY_OPT_VALUE = "any";
   
   /**
    * The "wildcard" option value.
    */
   public static final String WILDCARD_OPT_VALUE = "*";
   
   /**
    * The "CountMonitors" option value.
    */
   public static final String COUNT_MONITORS_OPT_VALUE = "CountMonitors";
  
   /**
    * The "requests" option value.
    */
   public static final String REQUESTS_OPT_VALUE = "requests";

   /**
    * The "logLevel" option value.
    */
   public static final String LOG_LEVEL_OPT_VALUE = "logLevel";
   
   /**
    * The "memSafe" option value.
    */
   public static final String MEM_SAFE_OPT_VALUE = "memSafe";

   /**
    * The "tail-log" operation name.
    */
   public static final String TAIL_LOG_CUSTOM_OP_NAME = "tail-log";

   /**
    * The "tail-count" operation name.
    */
   public static final String TAIL_COUNT_CUSTOM_OP_NAME = "tail-count";

   /**
    * The "CreateDir" operation name.
    */
   public static final String CREATE_DIR_OP_NAME = "CreateDir";

   /**
    * The "Dir" operation name.
    */
   public static final String DIR_OP_NAME = "Dir";

   /**
    * The "RemoveDir" operation name.
    */
   public static final String REMOVE_DIR_OP_NAME = "RemoveDir";

   /**
    * The "RemoveCheckpoint" operation name.
    */
   public static final String REMOVE_CHECKPOINT_OP_NAME = "RemoveCheckpoint";

   /**
    * The "RollbackCheckpoint" operation name.
    */
   public static final String ROLLBACK_CHECKPOINT_OP_NAME = "RollbackCheckpoint";

   /**
    * The "SaveCheckpoint" operation name.
    */
   public static final String SAVE_CHECKPOINT_OP_NAME = "SaveCheckpoint";

   /**
    * The "ChkName" operation name.
    */
   public static final String CHK_NAME_OP_NAME = "ChkName";

   /**
    * The "set-dir" custom operation name.
    */
   public static final String SET_DIR_CUSTOM_OP_NAME = "set-dir";

   /**
    * The "get-dir" operation name.
    */
   public static final String GET_DIR_CUSTOM_OP_NAME = "get-dir";

   /**
    * The "set-file" operation name.
    */
   public static final String SET_FILE_OP_NAME = "set-file";

   /**
    * The "set-files" operation name.
    */
   public static final String SET_FILES_CUSTOM_OP_NAME = "set-files";

   /**
    * The "get-file" operation name.
    */
   public static final String GET_FILE_OP_NAME = "get-file";

   /**
    * The "get-files" operation name.
    */
   public static final String GET_FILES_CUSTOM_OP_NAME = "get-files";

   /**
    * The "get-filestore" operation name.
    */
   public static final String GET_FILESTORE_OP_NAME = "get-filestore";

   /**
    * The "set-config" operation name.
    */
   public static final String SET_CONFIG_OP_NAME = "set-config";

   /**
    * The "modify-config" operation name.
    */
   public static final String MODIFY_CONFIG_OP_NAME = "modify-config";

   /**
    * The "get-config" operation name.
    */
   public static final String GET_CONFIG_OP_NAME = "get-config";

   /**
    * The "del-config" operation name.
    */
   public static final String DEL_CONFIG_OP_NAME = "del-config";

   /**
    * The "do-export" operation name.
    */
   public static final String DO_IMPORT_OP_NAME = "do-import";
   
   /**
    * The "export.xml" default datapower-configuration file name.
    */
   public static final String DEFAULT_DP_CONFIG_FILE_NAME = "export.xml";

   /**
    * The "input-file" option name.
    */
   public static final String INPUT_FILE_OPT_NAME = "input-file";

   /**
    * The "do-export" operation name.
    */
   public static final String DO_EXPORT_OP_NAME = "do-export";

   /**
    * The "get-status" operation name.
    */
   public static final String GET_STATUS_OP_NAME = "get-status";

   /**
    * The "overwrite-files" operation name.
    */
   public static final String OVERWRITE_FILES_OP_NAME = "overwrite-files";

   /**
    * The "logtemp" DP directory name.
    */
   public static final String LOGTEMP_DIR_NAME = "logtemp";

   /**
    * The "local" DP directory name.
    */
   public static final String LOCAL_DIR_NAME = "local";

   /**
    * The "default-log" DP file name.
    */
   public static final String DEFAULT_LOG_FILE_NAME = "default-log";

   /**
    * The AMP response string identifier.
    */
   public static final String AMP_RESPONSE_IDENTIFIER = "Response ";

   /**
    * The SOMA response string identifier.
    */
   public static final String SOMA_RESPONSE_IDENTIFIER = ":response ";
}
