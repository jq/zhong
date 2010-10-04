/*
 * LimeXMLSchemaFieldExtractor.java
 *
 * Created on May 1, 2001, 1:23 PM
 */

package com.limegroup.gnutella.xml;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper class to extract field names from a schema document
 * Note: This class is incomplete. It works only for subset of schemas. 
 * Some standard API should be used when available.
 *<p>
 * Some of Many Limitations:
 * <ul>
 * <li>Cant's use IDREF 
 * </li>
 * <li> might have problems if same field name is used in two different
 * contexts in the schema document (attribute names are no problem)
 * </li>
 * <li>Will work only if schema is valid. If schema is invalid (has errors),
 * the result may be unpredictable 
 * </li>
 * <li> Doesn't resolve references to other schemas </li>
 * <li> simpleType tag shouldn't be defined independently </li>
 * </ul>
 * Its just a 'quick & dirty' approach to extract the field names. Whenever
 * available, a standard parser should be used for parsing schemas. It is 
 * beyond the scope of current project to implement a parser that works with
 * all the schemas.
 * @author  asingla
 */
class LimeXMLSchemaFieldExtractor
{
    
    /**
     * The map from names to corresponding SchemaFieldInfoList
     */
    private Map _nameSchemaFieldInfoListMap = new HashMap();
    
    /**
     * A dummy name to be used when there's no name for a field
     */
    private static final String DUMMY = "DUMMY";
    
    /**
     * A dummy name that can be used for a simple type
     */
    private static final String DUMMY_SIMPLETYPE = "DUMMY_SIMPLETYPE";
    
    /**
     * Set of primitive types (as per XML Schema specifications)
     */
    private static final Set PRIMITIVE_TYPES = new HashSet();;
    
    /**
     * A counter to generate unique number which can be appened to strings
     * to form unique strings
     */
    private int _uniqueCount = 1;
    
    /**
     * The last autogenerated name for 'complexType' element
     */
    private String _lastUniqueComplexTypeName = "";
    
    /**
     * The last autogenerated name for 'complexType' element
     */
    private SchemaFieldInfo _lastFieldInfoObject = null;
    
    /**
     * The field names that are referenced/used from some other field
     * (ie which can not be root element)
     */
    private Set _referencedNames = new HashSet();
    
    //initialize the static variables
    static
    {
        //fill it with primitive types
        PRIMITIVE_TYPES.add("xsi:string");
        PRIMITIVE_TYPES.add("string");
        PRIMITIVE_TYPES.add("xsi:boolean");
        PRIMITIVE_TYPES.add("boolean");
        PRIMITIVE_TYPES.add("xsi:float");
        PRIMITIVE_TYPES.add("float");
        PRIMITIVE_TYPES.add("xsi:double");
        PRIMITIVE_TYPES.add("double");
        PRIMITIVE_TYPES.add("xsi:decimal");
        PRIMITIVE_TYPES.add("decimal");
        PRIMITIVE_TYPES.add("xsi:timeDuration");
        PRIMITIVE_TYPES.add("timeDuration");
        PRIMITIVE_TYPES.add("xsi:recurringDuration");
        PRIMITIVE_TYPES.add("recurringDuration");
        PRIMITIVE_TYPES.add("xsi:binary");
        PRIMITIVE_TYPES.add("binary");
        PRIMITIVE_TYPES.add("xsi:uriReference");
        PRIMITIVE_TYPES.add("uriReference");
        PRIMITIVE_TYPES.add("xsi:ID");
        PRIMITIVE_TYPES.add("ID");
        PRIMITIVE_TYPES.add("xsi:IDREF");
        PRIMITIVE_TYPES.add("IDREF");
        PRIMITIVE_TYPES.add("xsi:ENTITY");
        PRIMITIVE_TYPES.add("ENTITY");
        PRIMITIVE_TYPES.add("xsi:NUMTOKEN");
        PRIMITIVE_TYPES.add("NUMTOKEN");
        PRIMITIVE_TYPES.add("xsi:Qname");
        PRIMITIVE_TYPES.add("Qname");
    }

    /**
     * Returns a list of fields in the passed document. 
     * @param document The XML Schema documnet from where to extract fields
     * @requires The document be a valid XML Schema without any errors
     * @return A list (of SchemaFieldInfo) containing all the fields in the 
     * passed document. 
     * @throws <tt>NullPointerException</tt> if the <tt>Document</tt> argument
     *  is <tt>null</tt>
     */
    public List getFields(Document document) {
        if(document == null) {
            throw new NullPointerException("null document");
        }

        //traverse the document and gather information
        Element root = document.getDocumentElement();
        traverse(root);
        
        //now get the root element below <xsd:schema>
        String rootElementName = getRootElementName();
        
        //create a list to store the field names
        List fieldNames = new LinkedList(); 
        
        //fill the list with field names
        fillWithFieldNames(fieldNames, 
                           (List)_nameSchemaFieldInfoListMap.get(rootElementName),
                           rootElementName);
        
        //return the list of field names
        return fieldNames;
    }
    
    
    /**
     * Fills the passed list of fieldnames with fields from
     * the passed fieldInfoList.
     * @param prefix The prefix to be prepended to the new fields
     * being added
     */
    private void  fillWithFieldNames(List fieldNames,
                                     List fieldInfoList,
                                     final String prefix) {
        //get the iterator over the elements in the fieldInfoList
        Iterator iterator = fieldInfoList.iterator();
        //iterate
        while(iterator.hasNext()) {
            //get the next SchemaFieldInfoPair
            SchemaFieldInfoPair fieldInfoPair = (SchemaFieldInfoPair)iterator.next();
            
            //get the field type set corresponding to this field pair's type
            List newSchemaFieldInfoList 
                = (List)_nameSchemaFieldInfoListMap.get(
                fieldInfoPair.getSchemaFieldInfo().getType());
            
            //get the field
            String field = fieldInfoPair.getField();
            
            //get the field info object for this field
            SchemaFieldInfo fieldInfo = 
                fieldInfoPair.getSchemaFieldInfo();

            //if datatype is not defined elsewhere in the schema (may be
            //because it is a primitive type or so)
            if(newSchemaFieldInfoList == null)
            {
                //if not a dummy field
                if(!isDummy(field))
                {
                    //set the field name in the field info
                    fieldInfo.setCanonicalizedFieldName(prefix 
                        + XMLStringUtils.DELIMITER + field);
                }
                else
                {
                    //else just add the prefix (without field, as the 
                    //field is a dummy)
                    
                    //set the field name in the field info
                    fieldInfo.setCanonicalizedFieldName(prefix);
                }
                
                //add to fieldNames
                fieldNames.add(fieldInfo);
            }
            else
            {
                //else (i.e. when the datatype is further defined)
                
                //if not a dummy field
                if(!isDummy(field))
                {
                    //recursively call the method with the new values
                    //change the prefix to account for the field
                    fillWithFieldNames(fieldNames,newSchemaFieldInfoList,
                        prefix + XMLStringUtils.DELIMITER
                        + field);
                }
                else
                {
                    //recursively call the method with the new values
                    //prefix is not changed (since the field is dummy)
                    fillWithFieldNames(fieldNames,newSchemaFieldInfoList,prefix);
                }
            }
        }
    }
    
    /**
     * Tests if the passed field is a dummy field
     * @return true, if dummy, false otherwise
     */
    private boolean isDummy(String field)
    {
        if(field.trim().equals(DUMMY))
            return true;
    
        return false;
    }
    
    
    /**
     * Returns the root element below <xsd:schema>
     */
    private String getRootElementName()
    {
        //get the set of keys in _nameSchemaFieldInfoListMap
        //one of this is the root element
        Set possibleRoots = ((HashMap)((HashMap)_nameSchemaFieldInfoListMap).clone()).keySet();
        
        //Iterate over set of _referencedNames
        //and remove those from possibleRoots
        Iterator iterator = _referencedNames.iterator();
        while(iterator.hasNext())
        {
            //remove from set of possibleRoots
            possibleRoots.remove(iterator.next());
        }
        
        //return the first element in the set
        Iterator possibleRootsIterator = possibleRoots.iterator();
        return (String)possibleRootsIterator.next();
    }
    

    /**
     * Traverses the given node as well as its children and fills in the
     * datastructures (_nameSchemaFieldInfoListMap, _referencedNames etc) using
     * the information gathered
     * @param n The node which has to be traveresed (along with its children)
     * @modifies this
     */
    private void traverse(Node n)
    {
        //get the name of the node
        String name = n.getNodeName();
        
        //if element
        if(isElementTag(name))
        {
            //process the element tag and gather specific information
           processElementTag(n);
           
           //get and process children
            NodeList children = n.getChildNodes();
            int numChildren = children.getLength();
            for(int i=0;i<numChildren; i++)
            {
                //traverse the child
                Node child = children.item(i);
                traverse(child);
            }
        }
        else if(isComplexTypeTag(name))
        {
            //if its a complex type tag, process differently.
            processComplexTypeTag(n);
        }
        else if(isSimpleTypeTag(name))
        {
            //check for enumeration
            processSimpleTypeForEnumeration(n, _lastFieldInfoObject);
        }
        else
        {
            //traverse children
            traverseChildren(n);
        }
    }
    
    
    /**
     * Processes the 'complexType' tag (gets the structure of a complex type)
     * @param n The node having 'complexType' tag 
     */
    private void processComplexTypeTag(Node n)
    {
        String name = _lastUniqueComplexTypeName;
        //get attributes
        NamedNodeMap  attributes = n.getAttributes();
        //get name attribute
        Node nameAttribute = attributes.getNamedItem("name");
        if(nameAttribute != null)
        {
            name = nameAttribute.getNodeValue();   
        }
        
        //get new field info list
        List fieldInfoList = new LinkedList();
        
        //get and process children
        NodeList children = n.getChildNodes();
        int numChildren = children.getLength();
        for(int i=0;i<numChildren; i++)
        {
            Node child = children.item(i);
            processChildOfComplexType(child,fieldInfoList);
        }
        
        //add mapping to _nameSchemaFieldInfoListMap
        _nameSchemaFieldInfoListMap.put(name, fieldInfoList);     
        
        //also add to the _referencedNames
        _referencedNames.add(name);
    }
    
    /**
     * Processes the child of a 'complexType' element
     * @param n The child to be processed
     * @param fieldInfoList The list to which information related to the child
     * is to be put
     * @modifies fieldInfoList
     */
    private void processChildOfComplexType(Node n, 
        List fieldInfoList)
    {
            //get the name of the node
            String nodeName = n.getNodeName();
            
            //if element
            if(isElementTag(nodeName))
            {
                processChildElementTag(n,fieldInfoList);
            }
            else if(isAttributeTag(nodeName))
            {
                processChildAttributeTag(n,fieldInfoList);
            }
            else
            {
                //get the child nodes of this node, and process them
                NodeList children = n.getChildNodes();
                int numChildren = children.getLength();
                for(int i=0;i<numChildren; i++)
                {
                    Node child = children.item(i);
                    processChildOfComplexType(child,fieldInfoList);
                }
            }
    }
    
    /**
     * Processes the child that has the "element' tag
     * @param n child node to be processed
     * @param fieldInfoList The set to which information related to the child
     * is to be put
     * @modifies fieldInfoList
     */
    private void processChildElementTag(Node n, List fieldInfoList)
    {
         //get attributes
        NamedNodeMap  attributes = n.getAttributes();
        
        //schema field info for this element
        SchemaFieldInfo schemaFieldInfo = null;
        
        //get name attribute
        Node nameAttribute = attributes.getNamedItem("name");
        if(nameAttribute == null)
        {
            //get ref attribute
            Node refAttribute = attributes.getNamedItem("ref");
        
            if(refAttribute == null)
            {
                //return, cant do anything
                return;
            }

            //get the ref name
            String refName = refAttribute.getNodeValue();
            
            //create schema field info
            schemaFieldInfo = new SchemaFieldInfo(refName);
            //add mapping to fieldInfoList
            fieldInfoList.add(new SchemaFieldInfoPair(refName, 
                schemaFieldInfo));
            
            //also add the refName to set of _referencedNames
            _referencedNames.add(refName);
        }
        else
        {
            String name = nameAttribute.getNodeValue();

            //get type attribute
            Node typeAttribute = attributes.getNamedItem("type");
            String typeName;
            if(typeAttribute != null)
            {
                typeName = typeAttribute.getNodeValue();
            }
            else
            {
                typeName = getUniqueComplexTypeName();

                //also store it in _lastUniqueComplexTypeName for future use
                _lastUniqueComplexTypeName = typeName;
            }
            
            //create schema field info
            schemaFieldInfo = new SchemaFieldInfo(removeNameSpace(typeName));
            
            //add mapping to fieldInfoList
            fieldInfoList.add(new SchemaFieldInfoPair(name, 
                schemaFieldInfo));   
            
            //initialize the _lastFieldInfoObject for enumeration types
            _lastFieldInfoObject = schemaFieldInfo;
            
            //traverse children
            traverseChildren(n);
            
        }

    }
    
    /**
     * Removes the namespace part from the passed string
     * @param typeName The string whose namespace part is to be removed
     * @return The string after removing the namespace part (if present).
     * For eg If the passed string was "ns:type", the returned value will
     * be "type"
     */
    private String removeNameSpace(String typeName)
    {
        //if no namespace part
        if(typeName.indexOf(':') == -1)
        {
            //return the original string
            return typeName;
        }
        else 
        {
            //return the part of the string without namespace
            return typeName.substring(typeName.indexOf(':') + 1);
        }
        
    }
    
    /**
     * Processes the attribute child element
     * @param n The node whose child needs to be processed
     * @param fieldInfoList The set to which information related to the child
     * is to be put
     * @modifies fieldInfoList
     */
    private void processChildAttributeTag(Node n, List fieldInfoList)
    {
        //get attributes
        NamedNodeMap attributes = n.getAttributes();
        
        //get name
        Node nameAttribute = attributes.getNamedItem("name");
        if(nameAttribute == null)
        {
            //cant do much, return
            return;
        }
       
        //append DELIMITER after name of the attribute (as per convention
        //@see XMLStringUtils
        String name = nameAttribute.getNodeValue() + XMLStringUtils.DELIMITER;
        
        //get type
        Node typeAttribute = attributes.getNamedItem("type");
        String typeName;
        if(typeAttribute == null)
        {
            typeName = DUMMY_SIMPLETYPE;
        }
        else
        {
            typeName = typeAttribute.getNodeValue();
        }
       
        //get fieldinfo object out of type
        SchemaFieldInfo fieldInfo = new SchemaFieldInfo(removeNameSpace(typeName));
        
        Node editableAttribute = attributes.getNamedItem("editable");
        if(editableAttribute != null) {
            if(editableAttribute.getNodeValue().equalsIgnoreCase("false"))
                fieldInfo.setEditable(false);
        }
        
        //test for enumeration
        processSimpleTypeForEnumeration(n, fieldInfo);
        
        //add the attribute to the fieldInfoList
        addAttributeSchemaFieldInfoPair(
            new SchemaFieldInfoPair(name, fieldInfo), fieldInfoList);
        
        
        //add mapping to fieldInfoList
//        fieldInfoList.addFirst(new SchemaFieldInfoPair(name, fieldInfo));   
        
    }
    
    
    /**
     * Adds the passed schemaFieldInfoPair (which came from some attribute
     * in schema to the passed fieldInfoList.
     * This is don eso that the client gets attributes before the other
     * child elements (Summet needs it), and also so that attributes remain
     * in order.
     */
    private void addAttributeSchemaFieldInfoPair(
        SchemaFieldInfoPair schemaFieldInfoPair,
        List fieldInfoList)
    {
        int attributeCount = 0;
        //iterate over the fieldInfoList
        for(Iterator iterator = fieldInfoList.iterator();
                iterator.hasNext();)
        {
            //get the next element in the list
            SchemaFieldInfoPair nextElement = 
                (SchemaFieldInfoPair)iterator.next();
            
            //if the element is an attribute
            if(isAttribute(nextElement.getField()))
            {
                //increment the count of attributes
                attributeCount++;
            }
            else
            {
                //break out of the loop (The attributes are placed only in 
                //the beginning of the fieldInfoList, before any other element)
                break;
            }
        }
        
        //now add the passed schemaFieldInfoPair after the existing
        //attributes
        fieldInfoList.add(attributeCount, schemaFieldInfoPair);
    }
    
    /**
     * Tests the given node if it has enumerative type. If yes, then 
     * records the info (enumerations) in the passed fieldInfo
     * object
     */
    private static void processSimpleTypeForEnumeration(Node n, 
        SchemaFieldInfo fieldInfo)
    {
        //iterate over the child nodes to check for enumeration
        NodeList children = n.getChildNodes();
        int numChildren = children.getLength();
        for(int i=0;i<numChildren; i++)
        {
            //get the child node
            Node child = children.item(i);
            //get the name of the node
            String nodeName = child.getNodeName();
            
            //if isnt an enumeration tag
            if(!isEnumerationTag(nodeName))
            {
                //process this node (a child of it may be enumeration
                //element
                processSimpleTypeForEnumeration(child, fieldInfo);
            }
            else
            {
                //get the value attribute 
                Node nameAttribute = child.getAttributes().getNamedItem("name");
                Node valueAttribute = child.getAttributes().getNamedItem("value");
                String name = null, value = null;
                if(nameAttribute != null)
                    name = nameAttribute.getNodeValue();
                if(valueAttribute != null)
                    value = valueAttribute.getNodeValue();
                
                //add the enumeration to fieldInfo
                if(value != null && !value.equals("")) {
                    if(name == null || name.equals(""))
                        fieldInfo.addEnumerationNameValue(value, value);
                    else
                        fieldInfo.addEnumerationNameValue(name, value);
                }
            }
        }
    }
    
    /**
     * traverses the children of the passed node
     */
    private void traverseChildren(Node n)
    {
        //get and process children
        NodeList children = n.getChildNodes();
        int numChildren = children.getLength();
        for(int i=0;i<numChildren; i++)
        {
            //traverse the child
            Node child = children.item(i);
            traverse(child);
        }
    }
    
    /** 
     * Tests if the given tag denotes a complex type
     * @return true, if is a complex type tag, false otherwise
     */
    private boolean isComplexTypeTag(String tag)
    {
        if(tag.trim().equals("complexType") 
            || tag.trim().equals("xsd:complexType"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /** 
     * Tests if the given tag denotes a simple type
     * @return true, if is a complex type tag, false otherwise
     */
    private boolean isSimpleTypeTag(String tag)
    {
        if(tag.trim().equals("simpleType") 
            || tag.trim().equals("xsd:simpleType"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /** 
     * Tests if the given tag denotes a attribute
     * @return true, if is an attribute tag, false otherwise
     */
    private boolean isAttributeTag(String tag)
    {
        if(tag.trim().equals("attribute") || tag.trim().equals("xsd:attribute"))
            return true;
        return false;
    }
    
    
     /**
     * Gathers information from the element tag and updates the element
      * name & type information in _nameSchemaFieldInfoListMap
     * @param n The element node that needs to be processed
     * @modifies this
     */
    private void processElementTag(Node n)
    {
        //get attributes
        NamedNodeMap  attributes = n.getAttributes();
        
        //get name attribute
        Node nameAttribute = attributes.getNamedItem("name");
        
        //return if doesnt have name attribute
        if(nameAttribute == null)
            return;
        
        //get the name of the element
        String name = nameAttribute.getNodeValue();
        
        //get type attribute
        Node typeAttribute = attributes.getNamedItem("type");
        String typeName;
        //if type is specified in the element tag
        if(typeAttribute != null)
        {
            //get the type name
            typeName = typeAttribute.getNodeValue();
        }
        else
        {
            //else assign a new unique name for this type
            typeName = getUniqueComplexTypeName();
            //also store it in _lastUniqueComplexTypeName for future use
            _lastUniqueComplexTypeName = typeName;
        }
        
       //add mapping to _nameSchemaFieldInfoListMap
       addToSchemaFieldInfoListMap(name, typeName); 
    }
    
    /**
     * @modifies _uniqueCount
     */
    private String getUniqueComplexTypeName()
    {
        return "COMPLEXTYPE___" + _uniqueCount++;
    }
    
    
    /**
     * Adds the mapping for the passed field to a new SchemaFieldInfoList,
     * containing a SchemaFieldInfo element initialized with the passed
     * typeName
     */
    private void addToSchemaFieldInfoListMap(String field, String typeName)
    {
        //get new fieldinfo list
        List fieldInfoList = new LinkedList();
        fieldInfoList.add(new SchemaFieldInfoPair(DUMMY, new SchemaFieldInfo(
            removeNameSpace(typeName))));
        
        //add mapping to _nameSchemaFieldInfoListMap
        _nameSchemaFieldInfoListMap.put(field, fieldInfoList);
        
        //add type name to the referenced names set
        _referencedNames.add(removeNameSpace(typeName));
    }
    
    /**
     * Tests if the passed tag is a element tag
     * @return true, if element tag, false otherwise
     */
    private static boolean isElementTag(String tag)
    {
        if(tag.trim().equals("element") || tag.trim().equals("xsd:element"))
            return true;
        return false;
    }
    
     /**
     * Tests if the passed tag is a enumeration tag
     * @return true, if enumeration tag, false otherwise
     */
    private static boolean isEnumerationTag(String tag)
    {
        if(tag.trim().equals("enumeration") 
            || tag.trim().equals("xsd:enumeration"))
            return true;
        return false;
    }
    
    /**
     * Tests if the passed string represents attribute as per the 
     * canonicalized field conventions
     * @return true, if attribute field, false otherwise
     */
    public boolean isAttribute(String field)
    {
        //return true if ends with the delimiter used to represent
        //attributes
       if(field.endsWith(XMLStringUtils.DELIMITER))
           return true;
       else
           return false;
    }

/**
 * Stores the field and corresponding field information
 */
private static class SchemaFieldInfoPair
{
    /**
     * Name of the field
     */
    private String _field;
    
    /**
     * Information pertaining to this field
     */
    private SchemaFieldInfo _fieldInfo;
    
    /**
     * creates a new SchemaFieldInfoPair using the passed values
     */
    public SchemaFieldInfoPair(String field, SchemaFieldInfo fieldInfo)
    {
        this._field = field;
        this._fieldInfo = fieldInfo;
    }
    
    public String getField()
    {
        return _field;
    }
    
    public SchemaFieldInfo getSchemaFieldInfo()
    {
        return _fieldInfo;
    }
    
    public String toString()
    {
        return "[" + _field + ":" + _fieldInfo + "]";
    }
}
 

}//end of class LimeXMLSchemaFieldExtractor
