package org.dpdirect.utils;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;

public class XPathUtils {

    private XPathUtils() { }

    /**
     * Validate an XPath expression.
     *
     * @param xpath String : the XPath expression to validate.
     */
    public static void validateXPathExpression(String xpath) throws XPathExpressionException {
        if (xpath == null || xpath.trim().isEmpty()) {
            throw new XPathExpressionException("XPath expression is null or empty");
        }

        javax.xml.xpath.XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.compile(xpath);
    }

    /**
     * Evaluate an XPath expression against an XML string.
     *
     * @param xml String : the XML string to evaluate.
     * @param xpath String : the XPath expression to evaluate.
     *
     * @return boolean : true if the XPath expression evaluates to true.
     */
    public static  boolean evaluateXPath(String xml, String xpath) throws XPathExpressionException {
        validateXPathExpression(xpath);

        try {
            javax.xml.xpath.XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.compile(xpath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            NodeList nodes = (NodeList)xPath.evaluate(xpath, doc, XPathConstants.NODESET);

            return nodes.getLength() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract a value from an XML string using an XPath expression.
     *
     * @param xml String : the XML string to extract the value from.
     * @param xpath String : the XPath expression to use to extract the value.
     * 
     * @return String : the value extracted from the XML string.
     * 
     * @throws XPathExpressionException : if the XPath expression is invalid
     * @throws ParserConfigurationException : if the XML parser configuration is invalid
     * @throws IOException : if an I/O error occurs
     * @throws SAXException : if a parsing error occurs
     */
    public static String extractValue(String xml, String xpath) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        validateXPathExpression(xpath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate(xpath, doc, XPathConstants.NODESET);

        if (nodes.getLength() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(nodes.item(i).getTextContent());
        }
        return sb.toString();
    }

}
