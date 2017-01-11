package org.dpdirect.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.impl.dv.util.Base64;
import org.dpdirect.schema.DocumentHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A collection of common File and IO utilities.
 * 
 * @author Tim Goodwill
 */
public class FileUtils {
	
	/**
     * A constants for buffer size used to read/write data
     */
    private static final int BUFFER_SIZE = 4096;


   /**
    * Loads properties based on a resource bundle name.
    * 
    * @param bundleName the resource bundle name.
    * 
    * @return loaded properties from the named resource bundle.
    */
   public static Properties loadPropertiesFromBundle(String bundleName) {
      ResourceBundle rb = ResourceBundle.getBundle(bundleName);
      Properties properties = new Properties();
      for (String key : rb.keySet()) {
         String value = rb.getString(key);
         properties.put(key, value);
      }
      return properties;
   }

   /**
    * Loads properties from a Java properties file.
    * 
    * @param propertiesFile the properties file.
    * @return a Properties object loaded from the input file.
    * @throws IOException if there is an IO error attempting to read or load the properties.
    */
   public static Properties loadProperties(File propertiesFile) throws IOException {
      Properties prop = new Properties();
      InputStream fis = new FileInputStream(propertiesFile);
      try {
         prop.load(fis);
      }
      finally {
         try {
            fis.close();
         }
         catch (Exception e) {
            // Ignore.
         }
      }
      return prop;
   }

   /**
    * Gets the Base64 encoded representation of a the byte content of a file.
    * 
    * @param filePath the path of the file.
    * @return the Base64 encoded representation of a the byte content of the file.
    * @throws IOException if there is an error reading or encoding the file content.
    */
   public static String getBase64FileBytes(String filePath) throws IOException {
      return Base64.encode(readFileBytes(filePath));
   }
   
   /**
    * Decoded a Base64-encoded string and return as a String.
    * 
    * @param base64String the input Base64 string.
    * @throws IOException if there is an error writing to the file.
    */
   public static String decodeBase64ToString(String base64String) throws IOException {
	   String str = new String(Base64.decode(base64String), "UTF-8");
       return str;
   }
   
   /**
    * Decoded a Base64-encoded string and writes the content to a file.
    * 
    * @param filePath the path of the file to write to.
    * 
    * @param base64String the input Base64 string.
    * @throws IOException if there is an error writing to the file.
    */
   public static void decodeBase64ToFile(String filePath,
                                         String base64String) throws IOException {
      File file = new File(filePath);
      if (!file.getParentFile().exists()) {
         file.getParentFile().mkdirs();
      }
      FileOutputStream fos = new FileOutputStream(filePath);
      try {
         byte[] decodedBytes = Base64.decode(base64String);
         fos.write(decodedBytes);
         fos.flush();
      }
      finally {
         try {
            fos.close();
         }
         catch (Exception e) {
            // Ignore.
         }
      }
   }
   
   /**
    * Writes string content to a file.
    * 
    * @param filePath the path of the file to write to.
    * @param String the input string.
    * @throws IOException if there is an error writing to the file.
    */
   public static void writeStringToFile(String filePath,
                                        String inputString) throws IOException {
      File file = new File(filePath);
      if (!file.getParentFile().exists()) {
         file.getParentFile().mkdirs();
      }
      FileOutputStream fos = new FileOutputStream(filePath);
      try {
         fos.write(inputString.getBytes());
         fos.flush();
      }
      finally {
         try {
            fos.close();
         }
         catch (Exception e) {
            // Ignore.
         }
      }
   }

   /**
    * Prompts the user on the current System console for username and password.
    * 
    * @return the username and password.
    */
   public static Credentials promptForLogonCredentials() {
      Console console = null;
      char[] pwd = null;
      String userName = null;
      String password = null;
      if ((console = System.console()) != null) {
         userName = console.readLine("[%s] ", "Username");
         pwd = console.readPassword("[%s] ", "Password");
         password = new String(pwd);
      }
      return new Credentials(userName, password.toCharArray());
   }

   /**
    * Prompts the user on the current System console with a continue/quit modal option.
    */
   public static void promptForContinue() {
      Console console = null;
      String answer = null;
      if ((console = System.console()) != null) {
         answer = console.readLine("[%s] ", "Continue y/n");
      }
      if (!(answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes"))) {
         System.exit(1);
      }
   }

   /**
    * Reads the byte content of a file.
    * 
    * @param filePath the path and name of the file to read.
    * 
    * @return the byte content of the file.
    * 
    * @exception IOException if there is an error reading the file.
    */
   public static byte[] readFileBytes(String filePath) throws IOException {
      FileInputStream inputStream = null;
      try {
         inputStream = new FileInputStream(filePath);
         return readInputStreamBytes(inputStream);
      }
      finally {
         if (null != inputStream) {
            try {
               inputStream.close();
            }
            catch (Exception e) {
               // Ignore
            }
         }
      }
   }

   /**
    * Reads the byte content of an <code>InputStream</code>.
    * 
    * @param inputStream the <code>InputStream</code> from which to read content.
    * 
    * @return the byte content of the <code>InputStream</code>.
    * 
    * @exception IOException if there is an error reading the <code>InputStream</code>.
    */
   public static byte[] readInputStreamBytes(InputStream inputStream) throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try {
         byte[] buffer = new byte[4096];
         int bytesRead = inputStream.read(buffer);
         while (bytesRead != -1) {
            outputStream.write(buffer, 0, bytesRead);
            bytesRead = inputStream.read(buffer);
         }
         return outputStream.toByteArray();
      }
      finally {
         outputStream.close();
      }
   }

   /**
    * Deletes a directory and all its contents.
    * 
    * @param dir the directory to delete.
    */
   public static void deleteDirectory(File dir) {
      if (null != dir) {
         if (dir.exists()) {
            File[] files = dir.listFiles();
            if (null != files) {
               for (File file : files) {
                  if (file.isDirectory()) {
                     deleteDirectory(file);
                  }
                  else {
                     file.delete();
                  }
               }
            }
         }
         dir.delete();
      }
   }

   /**
    * Traverses a directory tree and gets a List of all Files in all subdirectories; the List is sorted using
    * File.compareTo().
    * 
    * @param dir a directory from which to get the file listing.
    * @throws IOException if the directory does not exist or is not readable or if there is a general IO error getting
    *            the file listing.
    */
   public static List<File> getFilesFromDirectory(File dir) throws IOException {
      validateDirectory(dir);
      List<File> result = getFilesFromDirectoryUnsorted(dir);
      Collections.sort(result);
      return result;
   }

   /**
    * Tests if a directory exists, is a directory rather than a file, and can be read.
    * 
    * @param dir a directory object.
    * @return true if the directory is valid; otherwise throws an IOException stating the reason the directory is not
    *         valid.
    * @throws IOException if the directory is not valid.
    */
   public static boolean validateDirectory(File dir) throws IOException {
      try {
         if (null == dir) {
            throw new IOException("Null directory object.");
         }
         else if (!dir.isDirectory()) {
            throw new IOException("Is not a directory: " + dir);
         }
         else if (!dir.canRead()) {
            throw new IOException("Directory cannot be read: " + dir);
         }
         return true;
      }
      catch (Exception e) {
         throw new IOException("Invalid directory object. " + e.getMessage());
      }
   }

   /**
    * Extracts all the contents of a zip file to a given local directory.
    * 
    * @param filename the zip file to extract from.
    * @param destination the head directory to unzip to.
    * @throws IOException if there is an IO error attempting to unzip the file.
    */
   public static void extractZipFiles(File filename,
                                      File destination) throws IOException {
      ZipFile zipFile = new ZipFile(filename);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
         ZipEntry entry = (ZipEntry) entries.nextElement();
         if (entry.isDirectory()) {
            (new File(destination + "//" + entry.getName())).mkdir();
            continue;
         }
         InputStream inputStream = zipFile.getInputStream(entry);
         OutputStream outputStream = new BufferedOutputStream(
               new FileOutputStream(destination + "//" + entry.getName()));
         try {
            copyStreams(inputStream, outputStream);
         }
         finally {
            if (null != inputStream) {
               try {
                  inputStream.close();
               }
               catch (Exception e) {
                  // Ignore.
               }
            }
            if (null != outputStream) {
               try {
                  outputStream.flush();
                  outputStream.close();
               }
               catch (Exception e) {
                  // Ignore.
               }
            }
         }
      }
      zipFile.close();
   }

   /**
    * Extracts all the contents of a named zip file directory to a given local directory.
    * 
    * @param filename the zip file to extract from.
    * @param sourceDir the root path of the directory to extract from the zip file (allows for partial extracts).
    * @param destDir the head directory to unzip to.
    * @param overWrite flag to indicate whether to override any existing content.
    * @throws IOException if there is an IO error attempting to unzip the file.
    */
   public static void extractZipDirectory(String filename,
                                          String sourceDir,
                                          String destDir,
                                          boolean overWrite) throws IOException {
      File destination = new File(destDir);
      File tempFile = new File(filename);
      ZipFile zipFile = new ZipFile(tempFile);
      final String rootName = sourceDir.replace(":", "");

      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
         ZipEntry entry = (ZipEntry) entries.nextElement();
         String entryName = entry.getName().replace("\\", "/");
         if (entryName.contains(rootName)) {
            if (entry.isDirectory()) {
               File newDir = new File(destination + "//" + entryName.replace(rootName, ""));
               if (overWrite && !newDir.getAbsolutePath().equals(destination.getAbsolutePath())) {
                  deleteDirectory(newDir);
               }
               newDir.mkdirs();
               continue;
            }
            InputStream inputStream = zipFile.getInputStream(entry);
            OutputStream outputStream = new BufferedOutputStream(
                  new FileOutputStream(destination + "//" + entryName.replace(rootName, "")));
            try {
               copyStreams(inputStream, outputStream);
            }
            finally {
               if (null != inputStream) {
                  try {
                     inputStream.close();
                  }
                  catch (Exception e) {
                     // Ignore.
                  }
               }
               if (null != outputStream) {
                  try {
                     outputStream.flush();
                     outputStream.close();
                  }
                  catch (Exception e) {
                     // Ignore.
                  }
               }
            }
         }
      }
      zipFile.close();
      tempFile.delete();
   }
   
   /**
    * Compresses file(s) to a destination zip file
    * 
    * @param files file or directory
    * @param destZipFile The path of the destination zip file
 * @throws Exception 
    */
  public static void zipDirectoryForImport(String zipDirPath, File file, String destZipFile, String domainName) throws Exception {
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZipFile));
	  String basePath = file.getParentFile().getCanonicalPath();
	  String exportFileName = basePath + "/export.xml";
      Document exportDoc = DocumentHelper.generateDocument();
      Element rootElement = exportDoc.createElement("datapower-configuration");
      rootElement.setAttribute("version", "3");
      exportDoc.appendChild(rootElement);
      Element configurationElement = exportDoc.createElement("configuration");
      configurationElement.setAttribute("domain", domainName);
      rootElement.appendChild(configurationElement);
      Element filesElement = exportDoc.createElement("files");
      rootElement.appendChild(filesElement);
      
      if (file.isDirectory()) {
          addFolderToZip(zipDirPath, file, null, zos, exportDoc);
          
//          DocumentHelper.buildDocument(exportDoc, exportFileName);
//          File exportFile = new File(exportFileName);
//          addFileToZip(exportFile, zos);
          
          addDocumentToZip(exportDoc, zos, "export.xml");
          
      } else {
          addFileToZip(file, zos);
      }
      
      zos.flush();
      zos.close();
  }
  
  /**
   * Adds a directory to the current zip output stream
   * 
   * @param folder the directory to be  added
   * @param parentFolder the path of parent directory
   * @param zos the current zip output stream
   * @throws FileNotFoundException
   * @throws IOException
   */
   private static void addFolderToZip(String zipDirPath, File folder, String parentFolder,
           ZipOutputStream zos, Document exportDoc) throws FileNotFoundException, IOException {
	   
	   for (File file : folder.listFiles()) {
		   String ThisRelativePath;
		   if (null != parentFolder) {
			   ThisRelativePath = parentFolder + "/" + file.getName();
		   }
		   else {
			   ThisRelativePath = file.getName();
		   }
		   String zipSource = zipDirPath + "/" + ThisRelativePath;
		   String destName = zipSource.replaceFirst("/", ":///");
		   String destLocation = destName.substring(0, destName.indexOf(":"));
		   
           if (file.isDirectory()) {
               addFolderToZip(zipDirPath, file, ThisRelativePath, zos, exportDoc);
               continue;
           }
           else {
        	   Node filesElement = exportDoc.getElementsByTagName("files").item(0);
        	   Element fileElement = exportDoc.createElement("file");
        	   fileElement.setAttribute("name", destName);
        	   fileElement.setAttribute("location", destLocation);
        	   fileElement.setAttribute("src", zipSource);
        	   filesElement.appendChild(fileElement);
           }

           zos.putNextEntry(new ZipEntry(zipSource));

           BufferedInputStream bis = new BufferedInputStream(
                   new FileInputStream(file));

           long bytesRead = 0;
           byte[] bytesIn = new byte[BUFFER_SIZE];
           int read = 0;

           while ((read = bis.read(bytesIn)) != -1) {
               zos.write(bytesIn, 0, read);
               bytesRead += read;
           }

           zos.closeEntry();

       }
   }

   /**
    * Adds a file to the current zip output stream
    * 
    * @param file the file to be added
    * @param zos the current zip output stream
    * @throws FileNotFoundException
    * @throws IOException
    */
   private static void addFileToZip(File file, ZipOutputStream zos)
           throws FileNotFoundException, IOException {
       zos.putNextEntry(new ZipEntry(file.getName()));

       BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
               file));

       long bytesRead = 0;
       byte[] bytesIn = new byte[BUFFER_SIZE];
       int read = 0;

       while ((read = bis.read(bytesIn)) != -1) {
           zos.write(bytesIn, 0, read);
           bytesRead += read;
       }

       zos.closeEntry();
   }
   

   /**
    * Adds a file to the current zip output stream
    * 
    * @param file the file to be added
    * @param zos the current zip output stream
 * @throws Exception 
    */
   private static void addDocumentToZip(Document exportDoc, ZipOutputStream zos, String fileName)
           throws Exception {
	   DocumentHelper.addDocumentToZip(exportDoc, zos, fileName);
   }


   /**
    * Copies the InputStream into the OutputStream, until the end of the stream has been reached.<br/>
    * This method uses a buffer of 4096 kbyte.
    * 
    * @param in the InputStream from which to read.
    * @param out the OutputStream where the data is written to.
    * @throws IOException if a IOError occurs.
    */
   public static void copyStreams(InputStream in,
                                  OutputStream out) throws IOException {
      copyStreams(in, out, 4096);
   }

   /**
    * Copies the InputStream into the OutputStream, until the end of the stream has been reached.
    * 
    * @param in the InputStream from which to read.
    * @param out the OutputStream where the data is written to.
    * @param buffersize the buffer size.
    * @throws IOException if a IOError occurs.
    */
   public static void copyStreams(InputStream in,
                                  OutputStream out,
                                  int buffersize) throws IOException {
      // a buffer to read the file.
      final byte[] bytes = new byte[buffersize];

      int bytesRead = in.read(bytes);
      while (bytesRead > -1) {
         out.write(bytes, 0, bytesRead);
         bytesRead = in.read(bytes);
      }
   }

   /**
    * Normalises a directory path by replacing any backslash characters with forward slashes and optional trimming of
    * trailing separator character
    * 
    * @param dirPath the directory path
    * @param trimTrailingSep true if trailing separator character should be trimmed; false otherwise.
    * @return the normalised directory path; or null if the input directory path is null.
    */
   public static String normaliseDirPath(String dirPath,
                                         boolean trimTrailingSep) {
      if (null != dirPath) {
         // Normalise path separator characters
         String normDirPath = dirPath.replace("\\", "/");
         if (trimTrailingSep && dirPath.endsWith("/")) {
            normDirPath = dirPath.substring(0, dirPath.length() - 1);
         }
         return normDirPath;
      }
      else {
         return null;
      }
   }

   /**
    * Traverses a directory tree and gets a List of all Files in all subdirectories.
    * 
    * @param dir a directory from which to get the files.
    * @throws IOException if the directory does not exist or is not readable or if there is a general IO error getting
    *            the file listing.
    */
   private static List<File> getFilesFromDirectoryUnsorted(File dir) throws IOException {
      List<File> result = new ArrayList<File>();
      File[] filesAndDirs = dir.listFiles();
      for (File file : filesAndDirs) {
         result.add(file); // always add, even if directory
         if (file.isDirectory()) {
            // recurse sub directories
            List<File> childDirList = getFilesFromDirectoryUnsorted(file);
            result.addAll(childDirList);
         }
      }
      return result;
   }

}
