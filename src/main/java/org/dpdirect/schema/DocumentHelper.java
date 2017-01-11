package org.dpdirect.schema;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 * Helper methods for building or traversing the DOM tree
 * 
 * @author Tim Goodwill
 */
public class DocumentHelper {
   /**
    * This method returns the value for a node, which is part of its child.
    * 
    * @param aNode is the node for which the value is sought.
    * @return returns back the value for this node.
    */
   private static HashMap prefixTable = new HashMap();

   private static ArrayList docNodeList = null;
   
   /**
    * A constants for buffer size used to read/write data
    */
   private static final int BUFFER_SIZE = 4096;

   public static Document generateDocument() throws ParserConfigurationException {
      DocumentBuilderFactory domfactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = domfactory.newDocumentBuilder();
      return docBuilder.newDocument();
   }

   /**
    * Gets a DOM document from an InputStream. Using the default "javax.xml.parsers.DocumentBuilderFactory".
    * 
    * @param inputStream the XML document input stream.
    * 
    * @return a DOM document object.
    * 
    * @throws IOException if there is an IO error.
    * 
    * @throws SAXException if there is a SAX parsing error.
    * 
    * @throws ParserConfigurationException if there is a parser config error.
    */
   public static Document parseDocument(InputStream inputStream) throws IOException,
                                                                SAXException,
                                                                ParserConfigurationException {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      docFactory.setNamespaceAware(true);
      docFactory.setIgnoringElementContentWhitespace(false);
      return docFactory.newDocumentBuilder().parse(inputStream);
   }

   /**
    * Method to serialize a DOM to a given output file.
    * 
    * @param docContent Document : the DOM
    * @param aFilePath String : the full output file path
    * @throws Exception : if error condition occurs
    */
   public static void buildDocument(Document docContent,
                                    String aFilePath) throws Exception {
      DOMImplementationLS DOMImp = null;
      FileOutputStream FOS = null;

      if ((docContent.getFeature("Core", "3.0") != null) && (docContent.getFeature("LS", "3.0") != null)) {
         DOMImp = (DOMImplementationLS) (docContent.getImplementation()).getFeature("LS", "3.0");
      }

      LSOutput lso = DOMImp.createLSOutput();
      try {
         FOS = new FileOutputStream(aFilePath);
         lso.setByteStream((OutputStream) FOS);
      }
      catch (java.io.FileNotFoundException e) {
         System.err.println(e.getMessage());
      }

      // get a LSSerializer object
      LSSerializer lss = DOMImp.createLSSerializer();
      lss.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
      lss.getDomConfig().setParameter("discard-default-content", Boolean.FALSE);
      lss.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
      // do the serialization
      boolean result = lss.write(docContent, lso);
      FOS.close();
   }
   
   /**
    * Method to serialize a DOM to a given output file.
    * 
    * @param docContent Document : the DOM
    * @param aFilePath String : the full output file path
    * @throws Exception : if error condition occurs
    */
   public static void addDocumentToZip(Document docContent,
		   ZipOutputStream zos, String fileName) throws Exception {
	  zos.putNextEntry(new ZipEntry(fileName));
      DOMImplementationLS DOMImp = null;

      if ((docContent.getFeature("Core", "3.0") != null) && (docContent.getFeature("LS", "3.0") != null)) {
         DOMImp = (DOMImplementationLS) (docContent.getImplementation()).getFeature("LS", "3.0");
      }

      LSOutput lso = DOMImp.createLSOutput();
      lso.setByteStream((OutputStream) zos);
 
      // get a LSSerializer object
      LSSerializer lss = DOMImp.createLSSerializer();
      lss.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
      lss.getDomConfig().setParameter("discard-default-content", Boolean.FALSE);
      lss.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
      // do the serialization
      boolean result = lss.write(docContent, lso);
      zos.closeEntry();
   }

   /**
    * Method to serialize a DOM to a given output file.
    * 
    * @param docContent Document : the DOM
    * @param OutputStream
    * @throws Exception : if error condition occurs
    */
   public static void buildDocument(Document docContent,
                                    OutputStream aOut) throws Exception {
      DOMImplementationLS DOMiLS = null;
      FileOutputStream FOS = null;

      if ((docContent.getFeature("Core", "3.0") != null) && (docContent.getFeature("LS", "3.0") != null)) {
         DOMiLS = (DOMImplementationLS) (docContent.getImplementation()).getFeature("LS", "3.0");
      }

      LSOutput lso = DOMiLS.createLSOutput();
      lso.setByteStream(aOut);
      // get a LSSerializer object
      LSSerializer lss = DOMiLS.createLSSerializer();
      lss.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
      lss.getDomConfig().setParameter("discard-default-content", Boolean.FALSE);
      lss.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
      // do the serialization
      boolean ser = lss.write(docContent, lso);
      FOS.close();
   }

   /**
    * Method to serialize a DOM to string.
    * 
    * @param docContent Document : the DOM
    * @param aFilePath String : the full output file path
    * @throws Exception : if error condition occurs
    */
   public static String buildDocumentString(Document docContent) throws Exception {
      DOMImplementationLS DOMImp = null;

      if ((docContent.getFeature("Core", "3.0") != null) && (docContent.getFeature("LS", "3.0") != null)) {
         DOMImp = (DOMImplementationLS) (docContent.getImplementation()).getFeature("LS", "3.0");
      }

      // get a LSSerializer object
      LSSerializer LSS = DOMImp.createLSSerializer();
      // lss.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
      LSS.getDomConfig().setParameter("discard-default-content", Boolean.FALSE);
      LSS.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
      // do the serialization
      return LSS.writeToString(docContent);
   }
   
   /**
    * Method to serialize a DOM Node to string.
    * 
    * @param node Node : the Node
    * @throws Exception : if error condition occurs
    */
   public static String buildNodeString(Node node) throws Exception {
	   Document document = node.getOwnerDocument();
	   DOMImplementationLS domImplLS = (DOMImplementationLS) document
	       .getImplementation();
	   LSSerializer LSS = domImplLS.createLSSerializer();
	   LSS.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
	   LSS.getDomConfig().setParameter("discard-default-content", Boolean.FALSE);
	   LSS.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
	   return LSS.writeToString(node);
   }

   /**
    * Method to return 'pretty-print' representation of XML.
    * 
    * @param XMLString String : Valid XML representation of a document.
    * @return 'pretty-print' string representation of valid XML document.
    * 
    * @throws Exception : if error condition occurs
    */
   public static String prettyPrintXML(String xmlString) throws Exception {
      Document responseDoc = parseDocument(new ByteArrayInputStream(xmlString.getBytes()));
      DOMImplementationLS DOMImp = null;
      if ((responseDoc.getFeature("Core", "3.0") != null) && (responseDoc.getFeature("LS", "3.0") != null)) {
         DOMImp = (DOMImplementationLS) (responseDoc.getImplementation()).getFeature("LS", "3.0");
      }
      // get a LSSerializer object
      LSSerializer LSS = DOMImp.createLSSerializer();
      LSS.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
      LSS.getDomConfig().setParameter("discard-default-content", Boolean.FALSE);
      LSS.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
      // do the serialisation
      return LSS.writeToString(responseDoc);
   }

   /**
    * Method to wrap plain text with in a named element to return valid XML.
    * 
    * @param textString String : plain text string.
    * @param rootElementName String : the name of the root element in which text is wrapped.
    * @return string representation of valid XML document.
    * 
    * @throws Exception : if error condition occurs
    */
   public static String stringAsXML(String textString,
                                    String rootElementName) throws Exception {
      Document responseDoc = null;
      String XMLwrappedString = null;
      try {
         responseDoc = generateDocument();
         Element rootElement = responseDoc.createElement(rootElementName);
         rootElement.setTextContent(textString);
         responseDoc.appendChild(rootElement);
         XMLwrappedString = responseDoc.toString();
      }
      catch (ParserConfigurationException ex) {

      }
      return buildDocumentString(responseDoc);
   }

   /**
    * This method assigns a new value to a node.
    * 
    * @param aNode is the node for which the new value is assigned
    * @param asNewValue is the new value
    */
   public static void setNodeTextValue(Node aNode,
                                       String asNewValue) {
      if (aNode instanceof Element) {
         if (aNode.getFirstChild() == null || aNode.getFirstChild().getNodeType() != org.w3c.dom.Node.TEXT_NODE)
            aNode.appendChild(aNode.getOwnerDocument().createTextNode(asNewValue));
         else
            aNode.getFirstChild().setNodeValue(asNewValue);
      }
      else if (aNode instanceof Attr) {
         Attr attribute = (Attr) aNode;
         attribute.setValue(asNewValue);
      }
   }

   /**
    * This method assigns a CDATA value to a node.
    * 
    * @param aNode is the node for which the new value is assigned
    * @param asNewValue is the new value
    */
   public static void setNodeCDValue(Node aCDataSectionNode,
                                     String asNewValue) {
      if (aCDataSectionNode.getFirstChild() == null
          || aCDataSectionNode.getFirstChild().getNodeType() != org.w3c.dom.Node.TEXT_NODE)
         aCDataSectionNode.appendChild(aCDataSectionNode.getOwnerDocument().createCDATASection(asNewValue));
      else
         aCDataSectionNode.getFirstChild().setNodeValue(asNewValue);
   }

   /**
    * Static method to create a DOM element.
    * 
    * @param aDoc is the document
    * @param asNameSpaceURI is the name space for the element. Will be null for DOM level 1 elements
    * @param asLocalName is the element name
    */
   public static Element createElement(Document aDoc,
                                       String asNameSpaceURI,
                                       String asLocalName) {
      Element newElem = null;
      if (asNameSpaceURI != null) {
         newElem = aDoc.createElementNS(asNameSpaceURI, asLocalName);
         // create prefix
         String[] parts = asNameSpaceURI.split("/");
         String lastPart = parts[parts.length - 1];
         String prefix;
         if ((lastPart.length() > 2) && (!lastPart.contains("."))) {
            prefix = lastPart.toLowerCase().substring(0, 3);
         }
         else {
            prefix = "tns";
         }
         int prefixSuffix = 0;
         do {
            if (prefixSuffix > 0) {
               prefix = prefix + String.valueOf(prefixSuffix);
            }
            if ((prefixTable.get(prefix) == null) || (prefixTable.get(prefix) == asNameSpaceURI)) {
               newElem.setPrefix(prefix);
               prefixTable.put(prefix, asNameSpaceURI);
               break;
            }
            else {
               prefixSuffix = prefixSuffix + 1;
            }
         }
         while (prefixTable.get(prefix) == null);
         aDoc.normalizeDocument();
      }
      else {
         newElem = aDoc.createElement(asLocalName);
      }
      return newElem;
   }

   /**
    * Static method to create a DOM attribute.
    * 
    * @param aDoc is the document
    * @param asNameSpaceURI is the name space for the attribute. Will be null for DOM level 1 attribute
    * @param asLocalName is the attribute name
    */
   public static Attr createAttribute(Document aDoc,
                                      String asNameSpaceURI,
                                      String asLocalName) {
      Attr newAttr = null;
      newAttr = aDoc.createAttribute(asLocalName);
      return newAttr;
   }

}
