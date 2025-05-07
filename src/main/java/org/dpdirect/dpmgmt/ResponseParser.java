package org.dpdirect.dpmgmt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 
import javax.xml.parsers.ParserConfigurationException;

import org.dpdirect.schema.DocumentHelper;
import org.dpdirect.utils.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A utility class to parse SOMA and AMP (XML) responses from an IBM DataPower
 * device.
 * 
 * @author Tim Goodwill
 */
public class ResponseParser {

	/** Output types that may be specified as 'outputType'. */
	protected static enum OutputType {
		OUTPUT_PARSED, OUTPUT_LINES, OUTPUT_XML
	};

	/**
	 * Set the style of output for the eyeball (OUTPUT_PARSED), string
	 * manipulation (OUTPUT_LINES) or xml parsing (OUTPUT_XML).
	 * */
	private OutputType outputType = OutputType.OUTPUT_PARSED;
	
	private String indentBlock = "  ";

	private Document responseDoc = null;

	private Node xmlPayload = null;

	private String outputFile = null;

	private List<String> failureState = new ArrayList<String>();

	private String filter = null;

	private String filterOut = null;
	
	private String filterString = null;
	
	private String filterOutString = null;
	
	private Pattern filterPattern = null;
	
	private Pattern filterOutPattern = null;
	
	private boolean suppressResponse = false;

	private org.apache.log4j.Level resultLevel = org.apache.log4j.Level.INFO;

	/**
	 * Constructs a new <code>ResponseParser</code> object.
	 */
	public ResponseParser() {
	}

	/**
	 * @return the outputFile
	 */
	public String getOutputFile() {
		return this.outputFile;
	}

	/**
	 * @param fileName
	 *            the outputFile to set
	 */
	public void setOutputFile(String fileName) {
		this.outputFile = fileName;
	}

	/**
	 * @return the failureState
	 */
	public List<String> getFailureState() {
		return this.failureState;
	}

	/**
	 * @param failString
	 *            the failureState to set
	 */
	public void setFailureState(String failString) {
		if (null != failString) {
			this.failureState.add(failString);
		}
	}

	/**
	 * @param filterOut
	 *            the filterOut to set
	 */
	public void setFilterOut(String filterOutString) {
		if (null != filterOutString){
		    this.filterOut = filterOutString.trim();
		}
	}

	/**
	 * @return the filterOut
	 */
	public String getFilterOut() {
		return this.filterOut;
	}

	/**
	 * @return the filter
	 */
	public String getFilter() {
		return this.filter;
	}

	/**
	 * @param filter
	 *            the filter to set
	 */
	public void setFilter(String filterString) {
		if (null != filterString){
		    this.filter = filterString.trim();
		}
	}
	
	/**
	 * @return the filter
	 */
	public boolean getSuppressResponse() {
		return this.suppressResponse;
	}

	/**
	 * @param filter
	 *            the filter to set
	 */
	public void setSuppressResponse(boolean suppress) {
		this.suppressResponse = suppress;
	}

	/**
	 * @return the outputType
	 */
	public OutputType getOutputType() {
		return outputType;
	}

	/**
	 * @param type
	 *            the outputType to set
	 */
	public void setOutputType(OutputType type) {
		this.outputType = type;
	}

	/**
	 * @param type
	 *            the outputType to set
	 */
	public void setOutputType(String type) {
		for (OutputType t : OutputType.values()) {
			if (t.toString().endsWith(type.toUpperCase())) {
				outputType = t;
			}
		}
	}

	/**
	 * Parse an XML response string to detect error conditions and assign an
	 * error org.apache.log4j.Level.
	 * 
	 * @param responseString
	 *            a valid XML response string.
	 * @param failureState
	 *            an optional string in the response message that would indicate
	 *            an unexpected result eg. 'up' for status request following a
	 *            stop command.
	 * @return a list of results including the result test and result org.apache.log4j.Level.
	 * @throws Exception
	 */
	public List<Object> parseResponseMsg(String responseString) throws Exception {
		String resultText = "";

		if (suppressResponse) {
			List<Object> result = new ArrayList<Object>();
			result.add(resultLevel);
			result.add(resultText);
			return result;
		}

		List<String> errorConditions = new ArrayList<String>();
		errorConditions.add("error");
		errorConditions.add("Error");
		errorConditions.add("ERROR");
		errorConditions.add("failure");
		errorConditions.add("Authentication failure");
		errorConditions.addAll(failureState);

		responseDoc = DocumentHelper.parseDocument(new ByteArrayInputStream(responseString.getBytes()));
		xmlPayload = responseDoc.getFirstChild();

		Node bodyNode = responseDoc.getElementsByTagNameNS(
"http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0);
		if (bodyNode != null && bodyNode.getFirstChild() != null) {
			xmlPayload = bodyNode.getFirstChild();
		}

		Node resultNode = xmlPayload;
		String responseNamespace = resultNode.getNamespaceURI();
		String nodeName = resultNode.getLocalName();

		// Process response with no namespace
		if (responseNamespace == null) {
			if (nodeName.equals("HttpErrorResponse")) {
				resultLevel = org.apache.log4j.Level.FATAL;
			}
		}

		// Process response with SOAP namespace
		else if (responseNamespace.contains("http://schemas.xmlsoap.org/soap/envelope")) {
			if (responseDoc.getElementsByTagNameNS(responseNamespace, "Fault").getLength() > 0) {
				resultNode = responseDoc.getElementsByTagNameNS(responseNamespace, "Fault").item(0);
				resultLevel = org.apache.log4j.Level.FATAL;
			}
		}

		// Process response with AMP namespace
		else if (responseNamespace.contains("http://www.datapower.com/schemas/appliance/management")) {
			Node opStateNode = responseDoc.getElementsByTagNameNS(responseNamespace, "OpState").item(0);
			Node statusNode = responseDoc.getElementsByTagNameNS(responseNamespace, "Status").item(0);

			// OpState node
			if (opStateNode != null) {
				resultNode = opStateNode;
				if (errorConditions.contains(opStateNode.getFirstChild().getNodeValue())) {
					resultLevel = org.apache.log4j.Level.WARN;
				}
			}
			// Status node
			else if (statusNode != null && statusNode.hasChildNodes()) {
				resultNode = statusNode;

				if (errorConditions.contains(statusNode.getFirstChild().getNodeValue())) {
					resultLevel = org.apache.log4j.Level.WARN;
				}
			}
		}

		// Process response with SOMA namespace
		else if (responseNamespace.contains("http://www.datapower.com/schemas/management")) {
			Node statusNode = responseDoc.getElementsByTagNameNS(responseNamespace, "status").item(0);
			Node fileNode = responseDoc.getElementsByTagNameNS(responseNamespace, "file").item(0);
			Node resultTag = responseDoc.getElementsByTagNameNS(responseNamespace, "result").item(0);
			NodeList cfgResultList = responseDoc.getElementsByTagName("cfg-result");
			NodeList fileResultList = responseDoc.getElementsByTagName("file-result");
			Node responseNode = responseDoc.getElementsByTagNameNS(responseNamespace, "response").item(0);

			// Status node
			if (statusNode != null && statusNode.hasChildNodes()) {
				resultNode = statusNode;

				if (errorConditions.contains(statusNode.getFirstChild().getNodeValue())) {
					resultLevel = org.apache.log4j.Level.WARN;
				}
			}
			// File node
			else if (fileNode != null) {
				resultNode = fileNode;
				if (fileNode.getFirstChild() != null &&
						fileNode.getFirstChild().getNodeValue().equals("ERROR")) {
					resultLevel = org.apache.log4j.Level.FATAL;
				}
			}
			// Result node
			else if (resultTag != null) {
				resultNode = resultTag;

				Node errorLog = responseDoc.getElementsByTagName("error-log").item(0);
				if (errorLog != null) {
					resultNode = errorLog;
				}

				Node eventNode = responseDoc.getElementsByTagNameNS(responseNamespace, "log-event").item(0);
				if (resultNode.getFirstChild() != null &&
						resultNode.getFirstChild().getNodeValue() != null &&
						errorConditions.contains(resultNode.getFirstChild().getNodeValue().trim())) {
					resultLevel = org.apache.log4j.Level.FATAL;
				} else if (eventNode != null &&
						eventNode.getAttributes().getNamedItem("level") != null &&
						errorConditions.contains(eventNode.getAttributes()
								.getNamedItem("level")
								.getNodeValue())) {
					resultLevel = org.apache.log4j.Level.FATAL;
				}
			}
			// CfgResultList node
			else if (cfgResultList != null) {
				for (int i = 0; i < cfgResultList.getLength(); i++) {
					resultNode = cfgResultList.item(i);

					String status = resultNode.getAttributes().getNamedItem("status").getNodeValue();
					if (errorConditions.contains(status)) {
						resultLevel = org.apache.log4j.Level.FATAL;
						break;
					}
				}
			}
			// FileResultList node
			else if (fileResultList != null) {
				for (int i = 0; i < fileResultList.getLength(); i++) {
					resultNode = fileResultList.item(i);

					String status = resultNode.getAttributes().getNamedItem("result").getNodeValue();
					if (errorConditions.contains(status)) {
						resultLevel = org.apache.log4j.Level.FATAL;
						break;
					}
				}
			}
			// Response node
			else if (responseNode != null) {
				resultNode = responseNode;
				if (errorConditions.contains(responseNode.getFirstChild().getNodeValue())) {
					resultLevel = org.apache.log4j.Level.FATAL;
				}
			}
		}

		resultText = processResponse(resultNode);

		List<Object> result = new ArrayList<Object>();
		result.add(resultLevel);
		result.add(resultText);

		return result;
	}


	/**
	 * Return output from response XML based on selected output mode.
	 * 
	 * @param resultNode
	 *            Node : a valid Node.
	 * @param responseXML
	 *            String : the entire response string.
	 * @throws Exception
	 */
	public String processResponse(Node resultNode) throws Exception {
		String resultText = null;
			
		filterString = ".*(" + filter + ").*";
		filterOutString = ".*(" + filterOut + ").*";
		filterPattern = Pattern.compile(filterString);
		filterOutPattern = Pattern.compile(filterOutString);

		/*
		 * Process base64 encoded file payload
		 */
		if ("file".equalsIgnoreCase(resultNode.getLocalName())
				&& null != resultNode.getFirstChild()
				&& !resultNode.getFirstChild().getNodeValue().equals("ERROR")) {
			resultText = resultNode.getFirstChild().getNodeValue();
			resultText = FileUtils.decodeBase64ToString(resultText);
			resultText = filterLines(resultText);
			if (null != outputFile) {
				FileUtils.writeStringToFile(outputFile, resultText);
				resultNode.getFirstChild().setNodeValue(outputFile);
			} else {
				resultNode.getFirstChild().setNodeValue(resultText);
				if (!outputType.equals(OutputType.OUTPUT_XML)) {
					return resultText;
				}
			}
		}

		switch (outputType) {
		case OUTPUT_PARSED:
			if (responseDoc.getElementsByTagName("MessageCounts").getLength() > 0) {
				resultText = processMonitor(resultNode);
			} else if (responseDoc.getElementsByTagName("ObjectStatus")
					.getLength() > 3) {
				resultLevel = org.apache.log4j.Level.INFO;
				resultText = processStatii(resultNode);
			} else {
				resultText = processNameValue(resultNode);
			}
			break;
		case OUTPUT_LINES:
			resultText = processLines(resultNode);
			break;
		case OUTPUT_XML:
			resultText = DocumentHelper.buildNodeString(xmlPayload);
			break;
		default:
			resultText = processNameValue(resultNode);
			break;
		}

		if (null != outputFile) {
			FileUtils.writeStringToFile(outputFile, resultText);
		}
		return resultText;
	}

	/**
	 * Filter lines by supplied filter and filterOut strings
	 * 
	 * @param parsedText
	 *            String : The parsed text.
	 */
	public String filterLines(String parsedText) {
		StringBuffer outputLines = new StringBuffer();
		String[] lines = parsedText.split("\r\n|\r|\n");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			
			Matcher filterMatch = filterPattern.matcher(line);
			Matcher filterOutMatch = filterOutPattern.matcher(line);
			
			if (line.length() > 1
					&& (null == filter 
						|| Constants.NONE_OPT_VALUE == filter 
						|| filterMatch.find())
					&& (null == filterOut
						|| Constants.NONE_OPT_VALUE == filterOut 
						|| !filterOutMatch.find())) {
				outputLines.append(line).append("\n");
			}
		}
		return outputLines.toString().trim();
	}

	/**
	 * Return an List of element content by tag name
	 * 
	 * @param tagName
	 *            String : name of a valid XML Element Name.
	 */
	public List<String> getContent(String tagName) {
		NodeList contentList = responseDoc.getElementsByTagName(tagName);
		List<String> content = new ArrayList<String>();

		for (int i = 0; i < contentList.getLength(); i++) {
			content.add(responseDoc.getElementsByTagName(tagName).item(i)
					.getFirstChild().getNodeValue());
		}
		return content;
	}

	/**
	 * Return an List of file paths on the device from get-filset response XML
	 * 
	 * @param responseXML
	 *            String : a valid get-filset XML response string.
	 * @return the file paths.
	 */
	public List<String> parseGetFileset(String responseXML) throws IOException,
			ParserConfigurationException, SAXException {
		List<String> filePaths = new ArrayList<String>();
		responseDoc = DocumentHelper.parseDocument(new ByteArrayInputStream(
				responseXML.getBytes()));
		Node locationNode = responseDoc.getElementsByTagName("location")
				.item(0);
		if (locationNode != null) {
			getFilePaths(filePaths, locationNode);
		}
		return filePaths;
	}

	/**
	 * Recurse through file paths adding each to a list.
	 * 
	 * @param filePaths
	 *            the List of file paths.
	 * @param directoryNode
	 *            the next node in the XML tree.
	 */
	private void getFilePaths(List<String> filePaths, Node directoryNode) {
		String directoryName = directoryNode.getAttributes()
				.getNamedItem("name").getNodeValue();
		NodeList nodes = directoryNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node nextNode = nodes.item(i);
			if (nextNode.getLocalName().equals("file")) {
				String fileName = nextNode.getAttributes().getNamedItem("name")
						.getNodeValue();
				filePaths.add(directoryName + "/" + fileName);
			} else if (nextNode.getLocalName().equals("directory")) {
				getFilePaths(filePaths, nextNode);
			}
		}
	}
	
	/**
	 * Return the parent node of a record set (multiple node children of the same name)
	 * or null if no parent node found at the given depth.
	 * 
	 * @param node to recurse.
	 * @param the depth to recurse to.
	 * 
	 * @return parent node of record set, or null if none found.     
	 */
	private Node findRecordParent(Node node, int drillDepth){
		/*
		 * Look (drillDepth) nodes deep to find multiple records (nodes of the same name).
		 */
		int childNodeCount = node.getChildNodes().getLength();
		
		for (int i = 0; i < childNodeCount; i++) {
			Node childNode = node.getChildNodes().item(i);
			String childNodeName = childNode.getLocalName();
			if (null != childNodeName) {
				if (responseDoc.getElementsByTagName(childNodeName)
						.getLength() > 1) {
					return node;
				}
			}
		}
		if (drillDepth > 0){
			for (int i = 0; i < childNodeCount; i++) {
				Node childNode = node.getChildNodes().item(i);
				Node foundNode = findRecordParent(childNode, drillDepth-1);
				if (foundNode != null){
					return foundNode;
				}
			}
		}
		return null;
	}

	/**
	 * Return a string of 'name: value' pairs extracted from XML response
	 * 
	 * @param parentNode
	 *            Node : first node in the XML tree to be parsed.
	 * 
	 * @return the concatenated result text.
	 */
	private String processNameValue(Node parentNode) {
		return recurseNameValue(parentNode, "", 1).replaceAll("\\n+$", "");
	}

	/**
	 * Recurse XML node tree to return 'name: value' pairs.
	 * 
	 * @param nextNode
	 *            the next node in the XML tree to be parsed.
	 * @param resultText
	 *            the concatenated result text.
	 * @param depth
	 *            the recursion depth. Controls indenting.
	 * 
	 * @return the concatenated result text.
	 */
	private String recurseNameValue(Node nextNode, String resultText, int depth) {
		
		String indentText = "";
		if (nextNode.getNodeType() == Node.ELEMENT_NODE) {
			// the entire text of the node and its descendants
			String nodeText = nextNode.getTextContent();
			
			Matcher filterMatch = filterPattern.matcher(nodeText);
			Matcher filterOutMatch = filterOutPattern.matcher(nodeText);
			
			Element parentElement = null;
			if (nextNode.getParentNode().getNodeType() == Node.ELEMENT_NODE) {
				parentElement = (Element) nextNode.getParentNode();
			}
			
			// if NOT multiple nodes of this name, or if multiple node (indicative of a record) satisfies regex
			if (parentElement == null || parentElement
					.getElementsByTagName(nextNode.getLocalName()).getLength()<2
					|| (null == filter 
							|| Constants.NONE_OPT_VALUE == filter 
							|| filterMatch.find())
						&& (null == filterOut
							|| Constants.NONE_OPT_VALUE == filterOut 
							|| !filterOutMatch.find())) {
				
				// indent based on node depth
				for (int i = 1; i < depth; i++) {
					indentText = indentText + indentBlock;
				}
	
				// Element name
				String nameEntry = indentText + nextNode.getLocalName();
				boolean hasText = (nextNode.getFirstChild() != null)
						&& (nextNode.getFirstChild().getNodeValue() != null);
				boolean hasChildren = nextNode.hasChildNodes();
				boolean hasAttributes = nextNode.hasAttributes();
				
				// append element name
				if (hasText || hasChildren || hasAttributes) {
					resultText = resultText + nameEntry;
					// process attributes
					resultText = concatAttributes(nextNode, resultText);
					// process child elements
					if (hasText) {
						resultText = resultText + ": "
								+ nextNode.getFirstChild().getNodeValue().trim();
					}	
					resultText = resultText + "\n";
				}
	
				if (hasChildren) { // recurse into child nodes
					depth += 1;
					NodeList childNodeList = nextNode.getChildNodes();
					for (int i = 0; i < childNodeList.getLength(); i++) {
						Node childNode = childNodeList.item(i);
						resultText = recurseNameValue(childNode, resultText, depth);
					}
				} else { // process siblings
					if (nextNode.getPreviousSibling() == null) { // only process
																	// siblings once
						Node nextSibling = nextNode.getNextSibling();
						if (nextSibling != null) {
							resultText = recurseNameValue(nextSibling, resultText,
									depth);
						}
					}
				}
			}
		}
		return resultText;
	}

	/**
	 * Return a string of concatenated log name/values extracted from get-log
	 * XML response
	 * 
	 * @param parentNode
	 *            Node : first node in the XML tree to be parsed.
	 * 
	 * @return the concatenated result text.
	 */
	private String processLogEntry(Node parentNode) {
		String lines = "";

		List<String> ignoreList = new ArrayList<String>();
		ignoreList.add("date");
		ignoreList.add("time");

		List<String> tabList = new ArrayList<String>();
		tabList.add("code");
		tabList.add("type");

		NodeList entries = responseDoc.getElementsByTagName("log-entry");
		for (int i = 0; i < entries.getLength(); i++) {
			String parsedEntry = recurseBlocksToLines(entries.item(i), "",
					ignoreList, tabList, 0);
			
			Matcher filterMatch = filterPattern.matcher(parsedEntry);
			Matcher filterOutMatch = filterOutPattern.matcher(parsedEntry);
			
			if (parsedEntry.length() > 1
					&& (null == filter 
						|| Constants.NONE_OPT_VALUE == filter 
						|| filterMatch.find())
					&& (null == filterOut 
						|| Constants.NONE_OPT_VALUE == filterOut 
						|| !filterOutMatch.find())) {
				lines = lines + "\n" + parsedEntry;
			}
		}
		return lines.trim();
	}

	/**
	 * Return a string of concatenated log name/values extracted from get-status
	 * XML response
	 * 
	 * @param parentNode
	 *            Node : first node in the XML tree to be parsed.
	 * 
	 * @return the concatenated result text.
	 */
	private String processStatii(Node parentNode) {
		String lines = "";

		List<String> ignoreList = new ArrayList<String>();
		ignoreList.add("ObjectStatus");

		List<String> tabList = new ArrayList<String>();
		tabList.add("Name");

		NodeList entries = parentNode.getChildNodes();
		for (int i = 0; i < entries.getLength(); i++) {
			String parsedEntry = recurseBlocksToLines(entries.item(i), "",
					ignoreList, tabList, 0);
			
			Matcher filterMatch = filterPattern.matcher(parsedEntry);
			Matcher filterOutMatch = filterOutPattern.matcher(parsedEntry);
			
			if (parsedEntry.length() > 1
					&& (null == filter
						|| Constants.NONE_OPT_VALUE == filter
					    || filterMatch.find())
			        && (null == filterOut 
			        	|| Constants.NONE_OPT_VALUE == filterOut 
			            || !filterOutMatch.find())) {
				lines = lines + "\n" + parsedEntry;
			}
		}

		if (OutputType.OUTPUT_PARSED.equals(getOutputType())) {
			lines = lines.replace(",\t", "\n ");
		}

		return lines.trim();
	}

	/**
	 * Return a string of concatenated log name/values extracted from
	 * contMonitor or Duration Monitor get-status XML response
	 * 
	 * @param parentNode
	 *            Node : first node in the XML tree to be parsed.
	 * 
	 * @return the concatenated result text.
	 */
	private String processMonitor(Node parentNode) {
		String lines = "";

		List<String> ignoreList = new ArrayList<String>();

		List<String> tabList = new ArrayList<String>();
		tabList.add("tenSeconds");

		NodeList entries = parentNode.getChildNodes();
		for (int i = 0; i < entries.getLength(); i++) {
			Node nextNode = entries.item(i);
			String concatValues = nextNode.getTextContent();
				
			Matcher filterMatch = filterPattern.matcher(concatValues);
			Matcher filterOutMatch = filterOutPattern.matcher(concatValues);
			
			if ((null == filter 
					|| Constants.NONE_OPT_VALUE == filter 
					|| filterMatch.find())
				&& (null == filterOut
					|| Constants.NONE_OPT_VALUE == filterOut 
					|| !filterOutMatch.find())){
			String parsedEntry = recurseBlocksToLines(nextNode, "",
					ignoreList, tabList, 0);
			if (parsedEntry.length() > 1) {
				if (parsedEntry.indexOf("\t") > -1) {
					parsedEntry = parsedEntry
							.substring(parsedEntry.indexOf("\t") + 1,
									parsedEntry.length());
				}

				lines = lines + parsedEntry;
			}
			}
		}

		return lines.trim();
	}

	/**
	 * Return a string of concatenated log name/values extracted from get-status
	 * XML response
	 * 
	 * @param parentNode
	 *            Node : first node in the XML tree to be parsed.
	 * 
	 * @return the concatenated result text.
	 */
	private String processLines(Node parentNode) {
		String lines = "";

		List<String> ignoreList = new ArrayList<String>();
		ignoreList.add("timestamp");

		List<String> tabList = new ArrayList<String>();

		/*
		 * Look 2 nodes deep to find multiple entries as line entry candidate.
		 */
		Node recordParent = findRecordParent(parentNode, 2);
		if (null == recordParent) {
			recordParent = parentNode;
		}

		NodeList entries = recordParent.getChildNodes();
		for (int i = 0; i < entries.getLength(); i++) {
			String parsedEntry = recurseBlocksToLines(entries.item(i), "",
					ignoreList, tabList, 0);
			
			Matcher filterMatch = filterPattern.matcher(parsedEntry);
			Matcher filterOutMatch = filterOutPattern.matcher(parsedEntry);
		
			if (parsedEntry.length() > 1
				&& (null == filter 
					|| Constants.NONE_OPT_VALUE == filter 
					|| filterMatch.find())
				&& (null == filterOut
					|| Constants.NONE_OPT_VALUE == filterOut 
					|| !filterOutMatch.find())) {
				lines = lines + "\n" + parsedEntry;
			}
		}

		return lines.trim();
	}

	/**
	 * Recurse XML node tree to return concatenated log name/values.
	 * 
	 * @param nextNode
	 *            Node : next node in the XML tree to be parsed.
	 * @param resultText
	 *            Node : the concatenated result text.
	 * @param depth
	 *            integer : the recursion depth. Controls indenting.
	 * 
	 * @return the concatenated result text.
	 */
	private String recurseBlocksToLines(Node nextNode, String resultText,
			List<String> ignoreList, List<String> tabList, int depth) {
		String indentText = "";
		if (nextNode.getNodeType() == Node.ELEMENT_NODE) {
			if (!ignoreList.contains(nextNode.getLocalName())) {
				if (depth > 0) {
					// line formatting
					if (tabList.contains(nextNode.getLocalName())) {
						resultText = resultText + "," + "\t";
					} else if (nextNode.hasChildNodes()
							&& !resultText.endsWith(", ")
							&& !(resultText.lastIndexOf('\n') == resultText
									.length() - 1)) {
						resultText = resultText + ", ";
					}

					// process elements
					if ((nextNode.getFirstChild() != null)
							&& (nextNode.getFirstChild().getNodeValue() != null)) {
						String newEntry = indentText
								+ nextNode.getLocalName()
								+ ": "
								+ nextNode.getFirstChild().getNodeValue()
										.trim();
						resultText = resultText + newEntry;
					} else if ((nextNode.hasChildNodes())) { // No text
						resultText = resultText + indentText
								+ nextNode.getLocalName();
						resultText = concatAttributes(nextNode, resultText);
					}
				}
				if (nextNode.getNextSibling() == null
						&& (!nextNode.hasChildNodes() || nextNode
								.getFirstChild().getNodeType() != Node.ELEMENT_NODE)) {
					resultText = resultText + "\n";
				}
			}
			if (nextNode.hasChildNodes()) { // recurse into child nodes
				depth = depth + 1;
				NodeList childNodeList = nextNode.getChildNodes();
				for (int i = 0; i < childNodeList.getLength(); i++) {
					Node childNode = childNodeList.item(i);
					resultText = recurseBlocksToLines(childNode, resultText,
							ignoreList, tabList, depth);
				}
			} else { // process children into child nodes
				if (nextNode.getPreviousSibling() == null) { // only process
																// siblings once
					Node nextSibling = nextNode.getNextSibling();
					if (nextSibling != null) {
						resultText = recurseBlocksToLines(nextSibling,
								resultText, ignoreList, tabList, depth);
					}
				}
			}
		}
		return resultText;
	}

	/**
	 * Retrieve and concatenate a node's attribute name/value pairs
	 * 
	 * @param nextNode
	 *            the next node in the XML tree to be parsed.
	 * @param resultText
	 *            the concatenated result text.
	 * 
	 * @return the concatenated result text.
	 */
	private String concatAttributes(Node nextNode, String resultText) {
		try {
			// process element attributes
			if (nextNode.getAttributes() != null) {
				NamedNodeMap attributes = nextNode.getAttributes();
				String attributesBlock = "";
				if (attributes.getNamedItem("name") != null) { // name attribute
																// first.
					attributesBlock = attributesBlock + "name="
							+ attributes.getNamedItem("name").getNodeValue();
					if (attributes.getLength() > 1) {
						attributesBlock = attributesBlock + ", ";
					}
				}
				// Clean up DP response namespace declaration.
				for (int i = 0; i < attributes.getLength(); i++) {
					if (!attributes.item(i).getNodeValue()
							.contains("soap-envelope")
							&& (attributes.item(i).getLocalName() != "name")) {
						attributesBlock = attributesBlock
								+ attributes.item(i).getLocalName() + "="
								+ attributes.item(i).getNodeValue();
						if (i != attributes.getLength() - 1) {
							attributesBlock = attributesBlock + ", ";
						}
					}
				}
				if (attributesBlock != "") {
					resultText = resultText + ", " + attributesBlock;
				}
			}
		} catch (NullPointerException ex) {
			return resultText;
		}
		return resultText;
	}

}
