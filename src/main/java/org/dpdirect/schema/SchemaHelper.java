package org.dpdirect.schema;

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
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;

/**
 * Class to find and tabulated schema elements.
 * 
 * Locate Root Element, Ancestor elements
 * 
 * Sample usage of the class is:
 * 
 * <pre>
 * SchemaHelper finder = new SchemaHelper(aModel);
 * 
 * List ancestors = finder.getAncestors(nodeName);
 * 
 * List nodes = finder.getRootNodes(ancestorList);
 * </pre>
 * 
 * @author Tim Goodwill
 */
public class SchemaHelper {

   protected XSModel schemaModel = null;

   /** internal implementation variables **/
   private Map<String, String> schemaMap = new HashMap<String, String>();

   private Map<String, String> parentMap = new HashMap<String, String>();

   private Map<String, XSElementDeclaration> rootNodeMap = new HashMap<String, XSElementDeclaration>();

   private List<String> ancestorList = new ArrayList<String>();

   private List<String> currentNodeNames = new ArrayList<String>();

   /*
    * Constructs a new <code>SchemaHelper</code> class.
    * 
    * @param schemaModel an XSModel object loaded from a schema file.
    */
   public SchemaHelper(XSModel schemaModel) {
      setSchemaModel(schemaModel);
      initialise();
   }

   /**
    * Method to initialise the object.
    * 
    * Builds schema (children) and parent tables
    */
   protected void initialise() {
      XSNamedMap map = schemaModel.getComponents(XSConstants.ELEMENT_DECLARATION);
      if (map.getLength() != 0) {
         for (int i = 0; i < map.getLength(); i++) {
            XSObject item = map.item(i);
            if (item instanceof XSElementDeclaration) {
               findReference((XSElementDeclaration) item);
            }
         }
      }
   }
   
   /**
    * Adds element name and referenced type to the schema map and adds element name and parent element
    * name to parent map.
    * 
    * @param key the name of a schema element
    * @param value the name of any element or complex type element.
    */
   private void addToSchemaMap(String key,
                               String value) {
      if (!key.equals(value)) {
         if (value != null) {
            if (parentMap.containsKey(value)) {
                String concatValues = parentMap.get(value);
                if (null != concatValues && 0 < concatValues.trim().length() && (-1 == concatValues.indexOf(key))) {
                   concatValues += ("," + key);
                   parentMap.put(value, concatValues);
                }
            }
            else {
                parentMap.put(value, key);
            }
            if (schemaMap.containsKey(key)) {
                String concatValues = schemaMap.get(key);
                if (null != concatValues && 0 < concatValues.trim().length() && (-1 == concatValues.indexOf(value))) {
                   concatValues += ("," + value);
                   schemaMap.put(key, concatValues);
                }
            }
            else {
               schemaMap.put(key, value);
            }
         }
         else if (!schemaMap.containsKey(key)) {
            schemaMap.put(key, "");
         }
      }
   }
   
   /**
    * @param schemaModel the schemaModel to set
    */
   public void setSchemaModel(XSModel schemaModel) {
      this.schemaModel = schemaModel;
   }

   /**
    * Gets the ancestor elements from a schema representation.
    * 
    * The returned vector will contain all the XSElementDeclaration ancestors of the target element.
    * 
    * @return List - a vector of XSElementDeclarations
 * @throws Exception 
    */
   public List<String> getAncestors(String nodePath) throws Exception {
      String parentPath = null;
	  String nodeName = nodePath;
	  if (nodeName.contains(".")) {
		  nodeName = nodePath.substring(nodePath.lastIndexOf("."), nodePath.length()-1);
		  parentPath = nodePath.substring(0 , nodePath.lastIndexOf("."));
	  }
      // find parent nodes - add to nodelist  
	  if (parentPath != null && parentPath.length()>0
			 && !ancestorList.contains(nodePath)) {
		 ancestorList.add(nodePath);
		 getAncestors(parentPath);
	  } 
	  else if (!ancestorList.contains(nodeName)){
		 ancestorList.add(nodeName);
		 String parentNodeNames = (String) parentMap.get(nodeName);
		 if (parentNodeNames != null) {
        	 String[] parentList = parentNodeNames.split(",");
        	 if (parentList.length==1) {
        		 if (!ancestorList.contains(parentList[0])) {
        			 getAncestors(parentList[0]);
        		 }
             }
        	 else {
        		 if (!ancestorList.contains(parentList[parentList.length-1])) {
        			 getAncestors(parentList[parentList.length-1]);
        		 }
//        		 throw new Exception("Multile nodes of name \'" + nodeName + "\'. Cannot resolve correct path.");
        	 }
        	 
//             for (String parentNodeName : parentList) {
//            	 if ((parentNodeName != null) && !ancestorList.contains(parentNodeName)) {
//            		 getAncestors(parentNodeName);
//                 }
//             }
        	 
         }
         else if ((nodeName != null) && (parentNodeNames == null)) {
        	 rootNodeMap.put(nodeName, getNode(nodeName));
         }
      }
      return ancestorList;
   }

   /**
    * Generate ancestor elements from set nodes where target element is not specifically nominated.
    * 
    * The returned vector will contain all the XSElementDeclaration ancestors of set elements. May produce unreliable
    * results where set element or attribute paths are short or name-concatValues pairs only. Setting a target node is
    * recommended where possible.
    * 
    * @return List - a vector of XSElementDeclarations
    */
   public void setAncestors(List<String> choiceList) {
      ancestorList = choiceList;
      for (String nodeName : choiceList) {
         if ((nodeName != null) && (parentMap.get(nodeName) == null)) {
            rootNodeMap.put(nodeName, getNode(nodeName));
         }
      }
   }

   public void resetAncestors() {
      ancestorList = new ArrayList<String>();
   }

   public List<String> getChildren(String nodeName) {
      ArrayList<String> children = new ArrayList<String>();
      String childrenAsString = (String) schemaMap.get(nodeName);
      if (childrenAsString != null) {
         String[] childList = childrenAsString.split(",");
         for (String s : childList) {
            children.add(s);
         }
      }
      return children;
   }

   /**
    * Return XSElementDeclaration for a named node.
    * 
    * @return XSElementDeclaration
    */
   public XSElementDeclaration getParentNode(String sNodeName) {
      // find parent nodes - add to nodelist
      String parentNode = (String) parentMap.get(sNodeName);
      if (parentNode.indexOf(",")>0) {
    	  return getNode(parentNode.substring(0, parentNode.indexOf(",")));
      }
      else {
    	  return getNode(parentNode);
      }
   }

   /**
    * Gets the qualified root elements from a schema representation. 
    * The root schema element has child elements, but is not itself a child of any element.
    * 
    * @return List - a vector of XSElementDeclaration's Ideally there should be only one element in the vector If the
    *         size is 0, it means no qualifying root found If size > 1, more than one element qualifies as root
    */
   public List<XSElementDeclaration> getRootNodes() {
      List<String> allNodes = new ArrayList<String>();
      allNodes.addAll(schemaMap.keySet());
      List<String> childNodes = new ArrayList<String>();
      childNodes.addAll(parentMap.keySet());
      allNodes.removeAll(childNodes);
      List<XSElementDeclaration> rootNodes = new ArrayList<XSElementDeclaration>();
      if (allNodes.size() != 0) {
         for (int i = 0; i < allNodes.size(); i++) {
            String item = (String) allNodes.get(i);
            rootNodes.add(i, getNode(item));
         }
      }
      return rootNodes;
   }

   /**
    * Gets the qualified root elements from a schema representation. 
    * The root schema element has child elements, but is not itself a child of any element.
    * 
    * @param nodeChoiceList a list of node names from which to select the root. The list will contain ancestors of the
    *           target node.
    * 
    * @return a list XSElementDeclaration objects. Depending on the design pattern of the schema there may be only one
    *         element in the vector If the size is 0, it means no qualifying root found If size > 1, more than one
    *         element qualifies as root.
    */
   public List<XSElementDeclaration> getRootNodes(List<String> nodeChoiceList) {
      List<String> rootNodeNames = new ArrayList<String>();
      List<XSElementDeclaration> eligibleNodes = new ArrayList<XSElementDeclaration>();
      // Return an empty list if the input or the underlying root node map is null or empty.
      if (null == nodeChoiceList || nodeChoiceList.size() < 1 || null == rootNodeMap || rootNodeMap.size() < 1) {
         return eligibleNodes;
      }
      rootNodeNames.addAll(rootNodeMap.keySet());
      rootNodeNames.retainAll(nodeChoiceList);
      for (String rootNodeName : rootNodeNames) {
         eligibleNodes.add(rootNodeMap.get(rootNodeName));
      }
      return eligibleNodes;
   }

   /**
    * Return the pre-determined root node as XSElementDeclaration list
    * 
    * @param localName the local name of the node
    * @return List
    */
   public List<XSElementDeclaration> getRootNodes(String localName) {
      List<XSElementDeclaration> nodeList = new ArrayList<XSElementDeclaration>();
      if (null == localName || 0 == localName.trim().length()) {
         // Return an empty node list.
         return nodeList;
      }
      XSElementDeclaration node = getNode(localName);
      if (null != node) {
         nodeList.add(node);
      }
      return nodeList;
   }

   /**
    * Return all nodes corresponding to a given local name
    * 
    * @param nodeName
    * @return List
    */
   public List<XSElementDeclaration> getNodes(String localName) {
      List<XSElementDeclaration> nodeList = new ArrayList<XSElementDeclaration>();
      if (null == localName || 0 == localName.trim().length()) {
         // Return an empty node list.
         return nodeList;
      }
      for (String key : schemaMap.keySet()) {
         if (key.equalsIgnoreCase(localName)) {
            nodeList.add(getNode(key));
         }
      }
      return nodeList;
   }

   /**
    * Returns a schema element XSElementDeclaration with the given local name. (This method is not namespace aware).
    * 
    * @param localName the local name of the node to fine.
    * @return the schema element or null if there is no such named element.
    */
   public XSElementDeclaration getNode(String localName) {
      if (null == localName || 0 == localName.trim().length()) {
         return null;
      }
      XSNamedMap map = schemaModel.getComponents(XSConstants.ELEMENT_DECLARATION);
      if (null != map) {
         for (int i = 0; i < map.getLength(); i++) {
            XSObject item = map.item(i);
            if (item instanceof XSElementDeclaration) {
               if (localName.equals(item.getName())) {
                  return (XSElementDeclaration) item;
               }
            }
         }
      }
      return null;
   }

   /**
    * Test if the schema contains an element declaration of a specified name
    * 
    * @param aNodeName String : the name of an element
    */
   public boolean nodeExists(String aNodeName) {
      return schemaMap.containsKey(aNodeName);
   }

   /**
    * Test each element name against the supplied regex and return matching nodes
    * 
    * @param regex String : the regex against which to match node names
    * 
    * @return return ArrayList of element names or empty ArrayList if none found
    */
   public List<String> findMatch(String regex) {
      String capRegex = regex.substring(0, 1).toUpperCase() + regex.substring(1, regex.length());
      String lowerRegex = regex.toLowerCase();
      List<String> nodeList = new ArrayList<String>();
      for (String nodeName : schemaMap.keySet()) {
         if (nodeName.matches(regex) || nodeName.equalsIgnoreCase(regex) || nodeName.contains(lowerRegex)
             || nodeName.contains(capRegex)) {
        	 String parentNodeNames = (String) parentMap.get(nodeName);
             if (parentNodeNames != null) {
            	 String[] parentList = parentNodeNames.split(",");
                 for (String parentNodeName : parentList) {
                	 String nodePath = parentNodeName + "." + nodeName;
                	 if (!nodeList.contains(nodePath)) {
                		 nodeList.add(nodePath);
                	 }
                 }
             }
             else {
            	 if (!nodeList.contains(nodeName)) {
            		 nodeList.add(nodeName);
            	 }
             }
         }
      }
      return nodeList;
   }

   /**
    * Finds all the element and complexType references for the specified root element and populates a map entry with
    * element names referenced by the element.
    * 
    * @param elementDeclaration XSElementDeclaration : the root element
    */
   private void findReference(XSElementDeclaration elementDeclaration) {
   	  XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
   	  String typeDefName = typeDefinition.getName();
      String thisContext = elementDeclaration.getName();
      if (null != typeDefinition) {
      	 currentNodeNames.clear();
      	 if (typeDefinition instanceof XSComplexTypeDefinition) {
            XSParticle particle = ((XSComplexTypeDefinition) typeDefinition).getParticle();   
            findReferenceInParticle(thisContext, particle);
         } else {
         	  addToSchemaMap(thisContext, typeDefName);
         }
      }
   }

   /**
    * Finds all the element and complexType references for the specified element and populates a map entry with element
    * names referenced by the element.
    * 
    * @param context the context.
    * @param elementDeclaration the element declaration.
    */
   private void findElementReference(String context, XSElementDeclaration elementDeclaration) {
      XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
      if (null != typeDefinition) {
      	 String typeDefName = typeDefinition.getName();
         String elementName = elementDeclaration.getName();
         addToSchemaMap(context, elementName);
         if (!(currentNodeNames.contains(elementName))) {
            currentNodeNames.add(elementName);
         }
         if (typeDefinition instanceof XSComplexTypeDefinition) {
            XSParticle particle = ((XSComplexTypeDefinition) typeDefinition).getParticle();
            if (null != particle) {
               findReferenceInParticle(elementName, particle);
            } else {
            	 addToSchemaMap(elementName, typeDefName);
            }
         } else { // add to keyset
            addToSchemaMap(elementName, typeDefName);
         }
      }
   }

   /**
    * Finds all the element and complexType references for a particle element and populates a map entry with element
    * names referenced by the element.
    * 
    * @param context the context.
    * @param particle the particle.
    */
   private void findReferenceInParticle(String context, XSParticle particle) {
      if (null != particle) {
         XSTerm term = particle.getTerm();
         if (term instanceof XSModelGroup) {
            XSObjectList xsObjectList = ((XSModelGroup) term).getParticles();
            for (int i = 0; i < xsObjectList.getLength(); i++) {
               XSObject xsObject = xsObjectList.item(i);
               if (xsObject instanceof XSParticle) {
                  findReferenceInParticle(context, (XSParticle) xsObject);
               }
            }
         }
         else if (term instanceof XSElementDeclaration) {
            findElementReference(context, (XSElementDeclaration) term);
         }
         else { // XSWildcard
            String termName = term.getName();
            if (termName != null) {
               addToSchemaMap(context, termName);
               if (!(currentNodeNames.contains(termName))) {
               	  currentNodeNames.add(termName);
               }
            } else {
            	 addToSchemaMap(context, "XSWildcard");
            }
         }
      }
   }

}
