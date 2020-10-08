package org.dpdirect.schema;

/**
 * Copyright 2016 Tim Goodwill
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xerces.dom.DOMXSImplementationSourceImpl;
import org.apache.xerces.impl.xs.util.StringListImpl;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * Generate an xml instance from a schema representation.
 * 
 * Generation requires identifying the root element in the schema from a nominated target element. Ancestor choices are
 * identified from the target element, however all optional elements and descendant choice must be identified in a
 * HashMap table via the setValues() method. Values may be null.
 * 
 * Sample usage of the SchemaModel class:
 * 
 * <pre>
 * SchemaModel somaModel = new SchemaModel(schemaFilePath, &quot;SetLogLevel&quot;);
 * somaModel.setSoapEnv();
 * somaModel.setAttributes(attributeMap);
 * somaModel.setValues(valueMap);
 * somaModel.generate(outputFilePath);
 * </pre>
 * 
 * @author Tim Goodwill
 */
public class SchemaLoader {

	/**
	 * Class logger.
	 */
	protected final Logger log = Logger.getLogger(this.getClass());

	/**
	 * Local resource path of the SOAP 1.1 XML schema.
	 */
	public static final String SOAP_11_SCHEMA_PATH = "/soap/1.1/soap-envelope.xsd";

	private XSObject xsRootElement = null;

	private XSModel xsModel = null;

	private Document generatedDoc = null;

	private DOMImplementationRegistry impRegistry = null;

	private SchemaHelper referenceNodes = null;

	private List<String> nodeChoiceList = new ArrayList<String>();

	private String schemaFileURI = null;

	private String targetNode = new String();

	private HashMap<String, ArrayList<String>> attributeValueMap = new HashMap<String, ArrayList<String>>();

	private HashMap<String, ArrayList<String>> enumerationMap = new HashMap<String, ArrayList<String>>();

	private HashMap<String, ArrayList<String>> textNodeValueMap = new HashMap<String, ArrayList<String>>();

	private boolean sampleXML = false;
	
	private String sampleRegex = null;

	private boolean soapEnv = false;

	private static final int DEFAULT_MAX_OCCURS = 1;

	private int maxValuePathDepth = 4;

	/**
	 * Constructor to build a SchemaLoader
	 * 
	 * @param schemaFileURI : a schema file URI.
	 * @throws Exception where an error occurs loading the schema 
	 *			or if the root element count in the schema is less thanone.
	 */
	public SchemaLoader(String schemaFileURI) throws Exception {
		this(schemaFileURI, null);
	}

	/**
	 * Constructor to build a Schema instance around a given nodeName
	 * 
	 * @param schemaFileURI : a schema file URI.
	 * @param nodeName : the local name of the target node.
	 * @throws Exception where an error occurs loading the schema 
	 *	        or if the root element count in the schema is less than one.
	 */
	public SchemaLoader(String schemaFileURI, String nodeName) throws Exception {
		this.setSchemaFileURI(schemaFileURI);
		targetNode = nodeName;
		// get DOM Implementation using DOM Registry
		System.setProperty(DOMImplementationRegistry.PROPERTY, DOMXSImplementationSourceImpl.class.getName());
		impRegistry = DOMImplementationRegistry.newInstance();
		XSImplementation impl = (XSImplementation) impRegistry.getDOMImplementation("XS-Loader");
		XSLoader schemaLoader = impl.createXSLoader(null);
		// schemaLoader.getConfig().setParameter("validate", Boolean.TRUE);
		String[] soapenvSchemaPath = { schemaFileURI, this.getClass().getResource(SOAP_11_SCHEMA_PATH).toExternalForm() };
		StringList schemaList = new StringListImpl(soapenvSchemaPath, 2);
		xsModel = schemaLoader.loadURIList(schemaList);
		this.referenceNodes = new SchemaHelper(xsModel);
		
		if (nodeName != null && 0 < nodeName.trim().length()) {
			nodeChoiceList = referenceNodes.getAncestors(nodeName);
		}
	}

	/**
	 * Gets the schemaFileURI.
	 * 
	 * @return the schemaFileURI.
	 */
	public String getSchemaURI() {
		return schemaFileURI;
	}

	/**
	 * Gets the targetNode.
	 * 
	 * @return the targetNode.
	 */
	public String getTargetNode() {
		return targetNode;
	}

	/**
	 * Document initialisation.
	 */
	public void newDocument() {
		referenceNodes.resetAncestors();
		textNodeValueMap.clear();
		attributeValueMap.clear();
		enumerationMap.clear();
		nodeChoiceList.clear();
		soapEnv = false;
		generatedDoc = null;
	}

	/**
	 * Sets the target node and generates the ancestor tree.
	 * 
	 * @param nodeName : the name of the target node to set.
	 * @throws Exception 
	 */
	public void setTargetNode(String nodeName) throws Exception {
		if (nodeName.contains(".")) {
			targetNode = nodeName.substring(nodeName.lastIndexOf("."), nodeName.length()-1);
		}
		else {
			targetNode = nodeName;
		}
		nodeChoiceList = referenceNodes.getAncestors(nodeName);
	}

	/**
	 * Creates a rudimentary soap envelope for the xml output.
	 * 
	 * Envelope and Body elements will be set. If desired, header may be set using the setValue method, eg.
	 * setValue("Envelope.Header.Security.UsernameToken", "A1B2C3")
	 */
	public void setSoapEnv() {
		soapEnv = true;
		nodeChoiceList.add("Envelope");
		nodeChoiceList.add("Body");
	}
	
	/**
	 * Strips a soap envelope from an xml tree.
	 * 
	 * @return a new root element.
	 */
	public XSElementDeclaration stripSoapEnv() {
		List<XSElementDeclaration> rootNodes = referenceNodes.getRootNodes(nodeChoiceList);
		if (rootNodes.size()>0){
			XSElementDeclaration newRoot = (XSElementDeclaration) rootNodes.get(0);
			setRootElement(newRoot);
			return newRoot;
		}
		return getRootElement();
	}

	/**
	 * Sets attribute values from a Map of key-value pairs.
	 * 
	 * @param attributeMap : a Map of attributeName-value pairs.
	 */
	public void setAttributeValues(HashMap<String, ArrayList<String>> attributeMap) {
		this.attributeValueMap = attributeMap;
	}

	/**
	 * Sets individual attribute values.
	 * 
	 * @param attrName : the attribute name. The name may consist of either a single attribute name, or dot delimited path
	 *			ELEMENT.ATTRNAME up to a maximum number ('maxValuePathDepth') elements deep. Method may be invoked
	 *			multiple times for any 1 attribute name or path to set multiple values for any attribute name or path,
	 *			resulting in multiple instances of the path 'popping' subsequent values off of the stack.
	 * @param attrValue the attribute value.
	 */
	public void setAttributeValue(String attrName, String attrValue) {
		if (attributeValueMap.containsKey(attrName)) {
			ArrayList<String> currentValue = attributeValueMap.get(attrName);
			currentValue.add(attrValue);
		} else {
			ArrayList<String> newArray = new ArrayList<String>();
			newArray.add(attrValue);
			attributeValueMap.put(attrName, newArray);
		}
		// handle "." delimited element/attribute path
		addToNodeChoiceList(attrName, true);
	}
	
	/**
	 * Add values to the nodeChoiceList for inclusion in the XML tree
	 * 
	 * @param nodeName : the node name. The name may consist of either a single node name, or dot delimited path
	 *			ELEMENT.ELEMENT up to a maximum number ('maxValuePathDepth') elements deep. 
	 */
	public void addToNodeChoiceList(String nodeName, boolean isAttribute){
		// handle "." delimited element/attribute path
		String[] elementList = nodeName.split("\\.");
		 
		int listLength;
		if (!isAttribute){
			listLength=elementList.length;
		 } else {
			// [length-1] last value is attribute, not element
			listLength=elementList.length-1;
		}
		
		String elementPath = "";
		for (int i = 0; i < listLength; i++) {
			for (int j = 0; j<=i ; j++) {
				if (j == 0) {
					elementPath = elementList[0];
				} else {
					elementPath = elementPath + "." + elementList[j];
				}
			}
			
			if (!nodeChoiceList.contains(elementPath)) {
				nodeChoiceList.add(elementPath);
			}
		}
	}
	
	/**
	 * Test whether values exist in the nodeChoiceList
	 * 
	 * @param nodeName String : a single node name 
	 * @param elementContext Node object : the parent node 
	 */
	public boolean choiceListContains(String nodeName, Node elementContext){
		boolean containsValue = false;
		int maxDepth = maxValuePathDepth;
		Node parentNode = elementContext;
		for (int i = 0; i < maxDepth; i++) {
			if (nodeChoiceList.contains(nodeName)){
				containsValue = true;
				break;
			}
			// construct a string representation of a path snippet of the
			// nodeName to attempt a more granular match. A more granular
			// match will overwrite a less granular match.
			if (parentNode != null) {
				 String parentNodeName = getNodeName(parentNode);
				 nodeName = parentNodeName + "." + nodeName;
				 parentNode = parentNode.getParentNode();
			} else {
				 break;
			}
		 }
		return containsValue;
	}

	/**
	 * Sets text node values from a Map of key-value pairs.
	 * 
	 * @param valueMap : a Map of elementName-value pairs.
	 */
	public void setTextNodeValues(HashMap<String, ArrayList<String>> valueMap) {
		this.textNodeValueMap = valueMap;
		nodeChoiceList.addAll(valueMap.keySet());
	}

	/**
	 * Sets individual text node values.
	 * 
	 * @param nodeName : the node name. The name may consist of either a single node name, or dot delimited path
	 *			ELMT1.ELMT2.ELMT3 up to a maximum number ('maxValuePathDepth') elements deep. Method may be invoked
	 *			multiple times for any 1 node name or path to set multiple values for any node name or path, resulting
	 *			in multiple instances of the path 'popping' subsequent values off of the stack.
	 * @param nodeTextValue : the text value of the node
	 */
	public void setTextNodeValue(String nodeName, String nodeTextValue) {
		if (textNodeValueMap.containsKey(nodeName)) {
			ArrayList<String> currentValue = textNodeValueMap.get(nodeName);
			currentValue.add(nodeTextValue);
		} else {
			ArrayList<String> newArray = new ArrayList<String>();
			newArray.add(nodeTextValue);
			textNodeValueMap.put(nodeName, newArray);
		}
		// handle "." delimited element path
		addToNodeChoiceList(nodeName, false);
	}

	/**
	 * Sets individual text node and/or attribute values
	 * 
	 * @param nodeOrAttrName : the node or attribute name. The name may consist of either a single global node or attribute
	 *			name, or a path delimited by "/" and/or "@" eg. ELMT/ELMT2/ELMT3 (set a Node Value), or
	 *			ELMT1/ELMT2@ATTRNAME (set an Attribute Value), or ELMT1.ELMT2@ATTRNAME (alternative notation). Path may
	 *			be up to 'maxValuePathDepth' elements deep (def 4). Path may dot delimited eg. ELMT1.ELMT2.NAME, however
	 *			in this case values are set for EITHER nodes or attributes with the corresponding name - not always
	 *			desirable. Method may be invoked multiple times for any 1 node name or path to set multiple values for
	 *			any node name or path, resulting in multiple instances of the path 'popping' subsequent values off of
	 *			the stack.
	 * @param nodeOrAttrValue : the text value of the node or attribute
	 */
	public void setValue(String nodeOrAttrName, String nodeOrAttrValue) {
		if (nodeOrAttrName.startsWith("//")) {
			nodeOrAttrName = nodeOrAttrName.substring(2, nodeOrAttrName.length());
		}
		
		if (nodeOrAttrName.startsWith("/")) {
			nodeOrAttrName = nodeOrAttrName.substring(1, nodeOrAttrName.length());
		}
		
		if (nodeOrAttrName.contains("@")) {
			// attribute path in the form of "EL1/EL2@ATTR" or "EL1.EL2@ATTR"
			if (nodeOrAttrName.startsWith("@")) {
				nodeOrAttrName = nodeOrAttrName.substring(1, nodeOrAttrName.length());
			}
			nodeOrAttrName = nodeOrAttrName.replace("@", ".");
			nodeOrAttrName = nodeOrAttrName.replace("/", ".");
			setAttributeValue(nodeOrAttrName, nodeOrAttrValue);
		} else if (nodeOrAttrName.contains("/")) {
			// element path in the form of "EL1/EL2/EL3"
			nodeOrAttrName = nodeOrAttrName.replace("/", ".");
			setTextNodeValue(nodeOrAttrName, nodeOrAttrValue);
		} else {
			// Global text values or "." delimited - "EL1.EL2.ELorATTR"
			// Set values for EITHER nodes or attributes - not always desirable
			setAttributeValue(nodeOrAttrName, nodeOrAttrValue);
			setTextNodeValue(nodeOrAttrName, nodeOrAttrValue);
		}
	}

	/**
	 * Gets a named attribute value of a node if one exists.
	 * 
	 * @param attrName : the attribute local name. The attribute object has not been constructed at this point.
	 * @param parentNode : the parent Node object, allows a string representation of a relative path snippet of the
	 *			attribute to be constructed.
	 * @return the value of the attribute if it exists.
	 */
	public String getAttributeValue(String attrName, Node parentNode) {
		String value = null;
		int maxDepth = maxValuePathDepth;

		for (int i = 0; i < maxDepth; i++) {
			if (attributeValueMap.containsKey(attrName)) {
				List<String> currentValue = attributeValueMap.get(attrName);
				if (currentValue != null && currentValue.size() > 1) {
					// multiple values - remove to cycle through
					if (currentValue.get(0) != null) {
						value = (String) currentValue.remove(0);
						currentValue.add(null);
					}
				} else if (currentValue != null && currentValue.size() > 0) {
					value = (String) currentValue.get(0);
				}
			}
			// construct a string representation of a path snippet of the
			// attribute to attempt a more granular match. A more granular
			// match will overwrite a less granular match.
			if (parentNode != null) {
				String parentNodeName = getNodeName(parentNode);
				attrName = parentNodeName + "." + attrName;
				parentNode = parentNode.getParentNode();
			} else {
				break;
			}
		}
		return value;
	}

	/**
	 * Gets a node text value if one has been set.
	 * 
	 * @param textNode : the Node object. Parent node is derived, allowing a string representation of a relative path
	 *			snippet to be constructed.
	 * @return the node text value if one has been set.
	 */
	public String getTextNodeValue(Node textNode) {
		String value = null;
		int maxDepth = maxValuePathDepth;

		String nodeName = getNodeName(textNode);  

		for (int i = 0; i < maxDepth; i++) {
			if (textNodeValueMap.containsKey(nodeName)) {
				List<String> currentValue = textNodeValueMap.get(nodeName);
				if (currentValue != null && currentValue.size() > 1) {
					// multiple values - remove to cycle through
					if (currentValue.get(0) != null) {
						value = (String) currentValue.remove(0);
						currentValue.add(null);
					}
				} else if (currentValue != null && currentValue.size() > 0) {
					value = (String) currentValue.get(0);
				}
			}
			// construct a string representation of a path snippet of the
			// attribute to attempt a more granular match. A more granular
			// match will overwrite a less granular match.
			if (textNode.getParentNode() != null) {
				textNode = textNode.getParentNode();
				String parentNodeName = getNodeName(textNode);
				nodeName = parentNodeName + "." + nodeName;
			} else {
				break;
			}
		}
		return value;
	}

	/**
	 * Gets the number of occurrences that have been set for a node.
	 * 
	 * @param nodeName : the Node name tested for the number of occurrences. The Node object has not been constructed
	 *			at this point.
	 * @param parentNode : the parent Node object, allows a string representation of a relative path snippet of the
	 *			attribute to be constructed.
	 * @return the number of values that have been set for a node.
	 */
	public int numberOfSetValues(String nodeName, Node parentNode) {
		int valNum = 0;
		int maxDepth = maxValuePathDepth;

		for (int i = 0; i < maxDepth; i++) {
			if (textNodeValueMap.containsKey(nodeName)) {
				List<String> currentValue = textNodeValueMap.get(nodeName);
				if (currentValue != null && !currentValue.isEmpty()) {
					valNum = currentValue.size();
				} else {
					valNum = 1;
				}
			}
			if (parentNode != null) {
				String parentNodeName = getNodeName(parentNode);
				nodeName = parentNodeName + "." + nodeName;
				parentNode = parentNode.getParentNode();
			} else {
				break;
			}
		}

		return valNum;
	}

	/**
	 * Determines if at least one node of a given name exists in the schema.
	 * 
	 * @param nodeName : the name of the node to check for.
	 * 
	 * @return true if there is such a node; false otherwise.
	 */
	public boolean nodeExists(String nodeName) {
		return referenceNodes.nodeExists(nodeName);
	}

	/**
	 * Gets a List of sample XML for nodes that match a given regular expression.
	 * 
	 * @param regex : a regular expression against which to match node names.
	 * 
	 * @return return a List of element names or empty List if none are found.
	 */
	public List<String> findMatch(String regex, boolean appendEnumeration) {
		this.sampleRegex = regex;
		List<String> nodeList = referenceNodes.findMatch(regex);
		List<String> xmlList = new ArrayList<String>();
		sampleXML = true;
		
		for (String nodeName : nodeList) {
			this.newDocument();
			try {
				this.setTargetNode(nodeName);
			} catch (Exception e1) {
				continue;
			}
			
			StringBuffer xmlSample = null;

			try {
				String documentString = generateDocumentString();
				if (null != documentString) {
					xmlSample = new StringBuffer(DocumentHelper.prettyPrintXML(documentString));
				}
			} catch (Exception e) {
				// Do Nothing
			}
			 
			if (null != xmlSample && appendEnumeration && !enumerationMap.isEmpty()) {
				for (String attrName : enumerationMap.keySet()) {
					xmlSample.append("\n# Valid ").append(attrName).append(" values:\n");
					List<String> enumList = enumerationMap.get(attrName);
					int count = 0;
					for (String enumValue : enumList) {
						if (count++ > 0) {
							xmlSample.append(", ");
						}
						xmlSample.append(enumValue);
					}
				}
				xmlSample.append("\n");
			}
			
			if (null != xmlSample && !xmlList.contains(xmlSample.toString())
					&& satisfiesRegex(xmlSample.toString())) {
				xmlList.add(xmlSample.toString());
			}
		}
		sampleXML = false;
		Collections.sort(xmlList);
		return xmlList;
	}
	
	protected boolean satisfiesRegex(String testString){
		String regex = this.sampleRegex;
		String capRegex = regex.substring(0, 1).toUpperCase() + regex.substring(1, regex.length());
		String lowerRegex = regex.toLowerCase();
		return (testString.matches(regex) 
				|| testString.contains(regex) 
				|| testString.contains(lowerRegex)
				|| testString.contains(capRegex));
	}

	/**
	 * Return a an XSObject representing the root schema element.
	 * 
	 * @param SchemaModel : the schema model as built from schema.
	 * 
	 * @return the root schema element.
	 * 
	 * @throws Exception if no root element can be located.
	 */
	protected XSObject getRootSchemaElement() throws Exception {
		if (null != this.xsModel) {
			List<XSElementDeclaration> rootNodes = null;
			if (nodeChoiceList != null) {
				if (soapEnv) {
					rootNodes = referenceNodes.getNodes("Envelope");
				} else {
					rootNodes = referenceNodes.getRootNodes(nodeChoiceList);
				}
			} else {
				rootNodes = referenceNodes.getRootNodes();
			}
			if (null == rootNodes || rootNodes.isEmpty()) {
			 log.error("Could not identify a root element.");
			}
			if (0 < rootNodes.size()) {
				if (1 < rootNodes.size()) {
					log.debug("Warning: Multiple root nodes detected in getRootSchemaElement() method. Returning the last in the list.");
				}
				return (XSElementDeclaration) rootNodes.get(rootNodes.size()-1);
			}
		}
		
		return null;
	}

	/**
	 * Gets an XSObject representing the named schema element.
	 * 
	 * @param nodeName : the name of the schema element.
	 * 
	 * @return the named schema element.
	 */
	protected XSObject getSchemaElement(String nodeName) throws Exception {
		List<XSElementDeclaration> namedNodes = new ArrayList<XSElementDeclaration>();
		namedNodes.add(getNode(nodeName));
		if (0 < namedNodes.size()) {
			if (1 < namedNodes.size()) {
				log.debug("Multiple nodes of the name '" + nodeName
							+ "' found in getSchemaElement() method. Returning the last in the list.");
			}
			return namedNodes.get(namedNodes.size()-1);
		} else if (namedNodes.isEmpty()) {
			log.warn("No element named '" + nodeName + "' was found in the schema declaration");
		}
		
		return null;
	}
	
	protected XSElementDeclaration getRootElement() {
		return (XSElementDeclaration) this.xsRootElement;
	}
	
	/**
	 * Sets an XSObject as the root schema element for the output xml document
	 * 
	 * @param xsRootSchemaElement : the root element to set.
	 */
	protected void setRootElement(XSObject xsRootSchemaElement) {
		this.xsRootElement = xsRootSchemaElement;
	}

	/**
	 * Gets an XSElementDeclaration for a named element.
	 * 
	 * @param nodeName : the name of a node.
	 * 
	 * @return the element XSElementDeclaration.
	 */
	protected XSElementDeclaration getNode(String nodeName) {
		XSNamedMap map = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
		if (map.getLength() != 0) {
			for (int i = 0; i < map.getLength(); i++) {
				XSObject item = map.item(i);
				if (item instanceof XSElementDeclaration) {
					String nextName = item.getName();
					if (nextName.equals(nodeName)) {
						return (XSElementDeclaration) item;
					}
				}
			}
		}
		
		return null;
	}

	/**
	 * Generates an xml instance for the Schema in the specified output file.
	 * 
	 * @param filePath : the path of the file to create.
	 * @throws Exception where an error occurs generating the document or writing to the file system.
	 */
	public void generateInstance(String filePath) throws Exception {
		if (null == targetNode) {
			referenceNodes.setAncestors(nodeChoiceList);
		}
		generatedDoc = DocumentHelper.generateDocument();
		setRootElement(getRootSchemaElement());
		if (null != xsRootElement) {
			parseSchema(xsRootElement, generatedDoc);
		}
		DocumentHelper.buildDocument(generatedDoc, filePath);
	}

	/**
	 * Generates an xml document for the Schema.
	 * 
	 * @throws Exception where an error occurs generating the xml document instance.
	 * @return returns the generated document string.
	 */
	public String generateDocumentString() throws Exception {
		if (null == targetNode) {
			referenceNodes.setAncestors(nodeChoiceList);
		}
		generatedDoc = DocumentHelper.generateDocument();
		setRootElement(getRootSchemaElement());
		if (null != xsRootElement) {
			parseSchema(xsRootElement, generatedDoc);
		}
		return DocumentHelper.buildDocumentString(generatedDoc);
	}

	/**
	 * Generates an xml instance for the Schema in the specified output file.
	 * 
	 * @param outputStream : an output stream to write the serialised document to.
	 * @throws Exception where an error occurs generating the xml document instance or writing to the output stream.
	 */
	public void generateInstance(OutputStream outputStream) throws Exception {
		generatedDoc = DocumentHelper.generateDocument();
		setRootElement(getRootSchemaElement());
		if (null != xsRootElement) {
			parseSchema(xsRootElement, generatedDoc);
		}
		DocumentHelper.buildDocument(generatedDoc, outputStream);
	}

	/**
	 * Start parsing a schema element and attach the result to the given node.
	 * 
	 * @param aSchemaElem XSElementDeclaration : the schema element
	 * @param aContext Node : the Node where the xml instance is generated
	 */
	protected void parseSchema(XSObject aSchemaElem, Node aContext) throws Exception {
		parseXSObject(aSchemaElem, aContext);
	}

	/**
	 * Parse a schema element and attach the result to the given node.
	 * 
	 * @param schemaElem : the schema element.
	 * @param nodeContext : the Node where the xml instance is generated.
	 * @return the generated DOM node.
	 */
	protected Node parseXSObject(XSObject schemaElem, Node nodeContext) throws Exception {
		Element contentElem = null;
		if (!(schemaElem instanceof XSSimpleTypeDefinition)) {
			// create the element
			contentElem = DocumentHelper.createElement(generatedDoc, schemaElem.getNamespace(), schemaElem.getName());
		}
		
		XSTypeDefinition tDefinition = null;
		
		if (schemaElem instanceof XSElementDeclaration) {
			tDefinition = ((XSElementDeclaration) schemaElem).getTypeDefinition();
			nodeContext.appendChild(contentElem);
		} else if (schemaElem instanceof XSTypeDefinition) {
			tDefinition = ((XSTypeDefinition) schemaElem);
		} else {
			tDefinition = ((XSTypeDefinition) schemaElem);
		}

		if (tDefinition instanceof XSComplexTypeDefinition) {
			XSComplexTypeDefinition ctDef = (XSComplexTypeDefinition) tDefinition;

			XSObjectList attList = ctDef.getAttributeUses();
			for (int i = 0; i < attList.getLength(); i++) {
				XSAttributeUse attrUseObject = (XSAttributeUse) attList.item(i);
				String attribname = attrUseObject.getAttrDeclaration().getName();
				if (sampleXML) {
					parseXSObject(attrUseObject.getAttrDeclaration().getTypeDefinition(),
									DocumentHelper.createElement(generatedDoc, null, attribname));
				}
				assignAttributeValue(attribname, contentElem);
			}

			XSParticle particle = ((XSComplexTypeDefinition) tDefinition).getParticle();

			String typeDefName = tDefinition.getName();
			if (null != typeDefName) {
				processXSParticle(particle, contentElem);
			} else {
				processXSParticle(particle, contentElem);
			}
		} else {
			if (sampleXML) {
				StringList enumeration = ((XSSimpleTypeDefinition) tDefinition).getLexicalEnumeration();
				if (0 < enumeration.getLength()) {
					String name;
					if (null == nodeContext.getParentNode()) {
						name = "'" + nodeContext.getNodeName() + "' attribute";
					}
					else {
						name = "'" + schemaElem.getName() + "' node";
					}
					ArrayList<String> enumList = enumerationMap.get(name);
					if (null == enumList) {
						enumList = new ArrayList<String>();
						enumerationMap.put(name, enumList);
					}
					for (int i = 0; i < enumeration.getLength(); i++) {
						enumList.add(enumeration.item(i));
					}
				}
			}
		}
		
		if (!(schemaElem instanceof XSSimpleTypeDefinition)) {
			assignNodeValue(contentElem);
		}
		
		return contentElem;
	}

	/**
	 * Processes an XSParticle.
	 * 
	 * @param particle : the particle to process.
	 * @param contentElem the DOM element to be populated.
	 */
	protected void processXSParticle(XSParticle particle, Element contentElem) throws Exception {
		if (null != particle && null != contentElem) {
			XSTerm term = particle.getTerm();
			if (term instanceof XSModelGroup) {
				processXSGroup((XSModelGroup) term, contentElem);
			} else if (term instanceof XSElementDeclaration) {
				String termName = term.getName();
				int numOccurs = getOccurances(particle, contentElem);
				if ((0 < numOccurs) || sampleXML) {
					Node currentNode = parseXSObject((XSElementDeclaration) term, contentElem);
					for (int i = 0; i < numOccurs - 1; i++) {
						Node newElem = currentNode.cloneNode(true);
						if (!(currentNode.getParentNode() instanceof Document)) {
							currentNode.getParentNode().appendChild(newElem);
						}
						NamedNodeMap attributes = newElem.getAttributes();
						for (int j = 0; j < attributes.getLength(); j++) {
							String attributeValue = getAttributeValue(attributes.item(j).getNodeName(), newElem);
							if (attributeValue != null) {
								attributes.item(j).setNodeValue(attributeValue);
							}
						}
						assignNodeValue(newElem);
					}
				}
			} else if (term instanceof XSWildcard) {
				if (soapEnv && xsRootElement.getName().equals("Envelope")
					 && getNodeName(contentElem).equals("Body")) {
					// soapEnv == true, and we have traversed down to the 'Body' element.
					// Now change xsRootSchemaElement and build payload.
					XSElementDeclaration newRoot = stripSoapEnv();
					if (null != newRoot){
						parseXSObject(newRoot, contentElem);
					}
				}
			} else {
				log.warn("Unprocessed term case:" + ((null == term) ? "" : term.getClass().toString()));
			}
		}
	}
	
	/**
	 * Processes an XSgroup.
	 * 
	 * @param xsModelGroup : the model group to process.
	 * @param elementContext : the DOM element to be populated.
	 */
	protected void processXSGroup(XSModelGroup xsModelGroup, Element elementContext) throws Exception {
		if (null != xsModelGroup && null != elementContext) {
			XSObjectList xsObjectList = xsModelGroup.getParticles();
			short groupType = xsModelGroup.getCompositor();
			boolean foundElement = false;
			if (XSModelGroup.COMPOSITOR_CHOICE == groupType) {
				for (int i = 0; i < xsObjectList.getLength(); i++) {
					XSObject choiceObject = xsObjectList.item(i);
					if (choiceObject instanceof XSParticle) {
						XSParticle particle = (XSParticle) choiceObject;
						String itemName = getParticleName(particle);
						if (itemName.contains(":")) {
							itemName = itemName.substring(itemName.lastIndexOf(":") + 1);
						}
						if (choiceListContains(itemName, elementContext)) {
					 	processXSParticle(particle, elementContext);
					 	foundElement = true;
						}
					}
				}
				if (sampleXML && !foundElement) {
					// generate all children
					for (int i = 0; i < xsObjectList.getLength(); i++) {
					XSParticle particle = (XSParticle) xsObjectList.item(i);
					if (satisfiesRegex(getNodeName(elementContext)) 
							|| satisfiesRegex(getParticleName(particle))) {
						processXSParticle((XSParticle) xsObjectList.item(i), elementContext);
					}
					}
				}
			} else {
				for (int i = 0; i < xsObjectList.getLength(); i++) {
					XSObject xsObject = xsObjectList.item(i);
					if (xsObject instanceof XSParticle) {
						processXSParticle((XSParticle) xsObjectList.item(i), elementContext);
					}
				}
			}
		}
	}

	/**
	 * Gets the 'standardised' name of a particle object
	 * 
	 * @param particle : the particle for which the name is to be returned.
	 * @return the particle name.
	 */
	public String getParticleName(XSParticle particle){
		String itemName = particle.toString();
		if (itemName.contains("{")){
			itemName = itemName.substring(0,itemName.indexOf("{"));
		}
		return itemName;
	}
	
	/**
	 * Sets the text value of an attribute on a provided element if set in the attributeValueMap table.
	 * 
	 * @param attName : the attribute local name.
	 * @param contentElem : the element to set the attribute on.
	 */
	public void assignAttributeValue(String attName, Element contentElem) {
		if (null != attName && null != contentElem) {
			String attValue = null;
			if (sampleXML) {
				attValue = "?";
			} else {
				attValue = getAttributeValue(attName, contentElem);
			}
			
			if (null != attValue) {
				Attr att = DocumentHelper.createAttribute(generatedDoc, "", attName);
				att.setValue(attValue);
				contentElem.setAttributeNode(att);
			}
		}
	}

	/**
	 * Sets the text node value of a provided element if set in the textNodeValueMap table
	 * 
	 * @param contentElem : the element to update.
	 */
	public void assignNodeValue(Node contentElem) {
		if (null != contentElem) {
			String nodeValue = getTextNodeValue(contentElem);
			if (null != nodeValue) {
				DocumentHelper.setNodeTextValue(contentElem, (nodeValue));
			}
		}
	}
	
	/**
	 * Gets the name of a node object
	 * 
	 * @param node : the element for which the name is to be returned.
	 * @return the node name.
	 */
	public String getNodeName(Node node){
		String nodeName = node.getLocalName();
		 if (nodeName == null) {
	 		nodeName = node.getNodeName();
		 }
		 return nodeName;
	}

	/**
	 * Gets the number of occurrences for a particular node based on the XSParticle reference in the schema.
	 * 
	 * @param particle : the particle reference.
	 * @param parentElem : the parent element.
	 * @return the actual no of calculated occurrences.
	 */
	public int getOccurances(XSParticle particle, Element parentElem) {
		if (null != particle && null != parentElem) {
			int minOccurs = particle.getMinOccurs();
			int maxOccurs = particle.getMaxOccurs();
			
			String particleName = particle.getTerm().getName();
			
			if (choiceListContains(particleName, parentElem)) {
				int numOfSetValues = this.numberOfSetValues(particleName, parentElem);
				return Math.max(1, numOfSetValues);
			} else if (0 == minOccurs) {
				return 0;
			} else if (minOccurs >= maxOccurs) {
				return minOccurs;
			}
		}
		
		return DEFAULT_MAX_OCCURS;
	}

	/**
	 * @return the schemaFileURI
	 */
	public String getSchemaFileURI() {
		return schemaFileURI;
	}

	/**
	 * @param schemaFileURI the schemaFileURI to set
	 */
	public void setSchemaFileURI(String schemaFileURI) {
		this.schemaFileURI = schemaFileURI;
	}

}
