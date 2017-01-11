package org.dpdirect.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A utility class for reading project properties.
 * 
 * @author Tim Goodwill
 */
public class DPDirectProperties {

   /**
    * The name and path of the project properties file.
    */
   public static final String PROJECT_PROP_FILE = "dpdirect.properties";

   /**
    * The name of the "netrc.file.path" property key.
    */
   public static final String NETRC_FILE_PATH_KEY = "netrc.file.path";
   
   /**
    * The name of the "netrc.file.path" property key.
    */
   public static final String NETRC_WIN_FILE_NAME = "_netrc";
   
   /**
    * The name of the "netrc.file.path" property key.
    */
   public static final String NETRC_UNIX_FILE_NAME = ".netrc";

   /**
    * The name of the "tail.log.lines" property key.
    */
   public static final String TAIL_LOG_LINES_KEY = "tail.log.lines";
   
   /**
    * The name of the "firmware.level" property key.
    */
   public static final String FIRMWARE_LEVEL_KEY = "firmware.level";
   
   /**
    * The name of the "firmware.level" property key.
    */
   public static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

   private boolean isWindows = true;
   /**
    * Cache of the project properties.
    */
   private Properties props = null;

   /**
    * Constructs a new <code>DPDirectProperties</code> object.
    * 
    * @throws IOException if there is an error reading or initialising the properties.
    */
   public DPDirectProperties() throws IOException {
      props = new Properties();
      InputStream inputStream = null;
      isWindows = System.getProperty("os.name").startsWith("Windows");
      try {
    	 //internal properties file
         inputStream = DPDirectProperties.class.getResourceAsStream("/" + PROJECT_PROP_FILE);
         props.load(inputStream);
         
         //external properties file
         String filePath = FileUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		 if (System.getProperty("os.name").startsWith("Windows")){
			filePath = filePath.substring(1, filePath.length());
		 }
		 File jarFile = new File(filePath);
		 String propFilePath = jarFile.getParent();
		 File propFile = new File(propFilePath + "/" + PROJECT_PROP_FILE);
		 if (propFile.exists()){
			 Properties externalProps = FileUtils.loadProperties(propFile);
			 props.putAll(externalProps);
		 }
      }
      catch (Exception e) {
         throw new IOException("Failed to load the project properties file '" + PROJECT_PROP_FILE + "'", e);
      }
      finally {
         if (null != inputStream) {
            try {
               inputStream.close();
            }
            catch (IOException e) {
               // Ignore.
            }
         }
      }
   }

   /**
    * Gets a property from the loaded object representation of the project properties file.
    * 
    * @param key the property key.
    * 
    * @return the property value or null if there is no such property.
    */
   public String getProperty(String key) {
	  if (null != props) {
		  String value = props.getProperty(key);
		  if (null == value && NETRC_FILE_PATH_KEY.equalsIgnoreCase(key)) {
			  if (isWindows) {
				  String homePath = System.getenv("USERPROFILE");
				  value = homePath + "/" + NETRC_WIN_FILE_NAME;
				  File netrcFile= new File(value);
				  if (null == value || !netrcFile.exists()) {
					  homePath = System.getenv("HOMESHARE");
					  value = homePath + "/" + NETRC_WIN_FILE_NAME;
					  netrcFile= new File(value);
				  }
				  if (null == value || !netrcFile.exists()) {
					  homePath = System.getenv("HOMEDRIVE");
					  value = homePath + "/" + NETRC_WIN_FILE_NAME;
				  }
				  return value;
			  }
			  else {
				  String homePath = System.getenv("HOME");
				  return homePath + "/" + NETRC_UNIX_FILE_NAME;
			  }
		  }
		  else {
		      return value;
		  }
      }
      return null;
   }
   
}

