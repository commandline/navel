/**
 * Copyright (c) 2003, Thomas Gideon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 *     * Neither the name of the Navel project team nor the names of its
 *       contributors may be used to endorse or promote products derived from this
 *       software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.sf.navel.beans;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.navel.example.ListBean;
import net.sf.navel.example.TypesBean;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author thomas
 * 
 */
public class InitialValuesIndexedBuilderTest
{

    private static final Logger LOGGER = LogManager
            .getLogger(InitialValuesIndexedBuilderTest.class);

    /*
     * Test method for 'net.sf.navel.beans.ListBuilder.filter(Class<?>, Map<String,
     * Object>)'
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilter()
    {
        Map<String, Object> rawValues = new TreeMap<String, Object>();

        TypesBean second = ProxyFactory.createAs(TypesBean.class);
        second.setInteger(2);
        TypesBean third = ProxyFactory.createAs(TypesBean.class);
        third.setInteger(3);

        rawValues.put("collection[0].boolean", true);
        rawValues.put("collection[1]", second);
        rawValues.put("collection[]", third);

        BeanInfo beanInfo = JavaBeanHandler.introspect(ListBean.class);

        Map<String, Object> filteredValues = new HashMap<String, Object>(
                rawValues);

        Map<String, PropertyDescriptor> properties = PropertyValues
                .mapProperties(beanInfo);

        InitialValuesIndexedBuilder.filter(properties, filteredValues);

        List<TypesBean> fooList = (List<TypesBean>) filteredValues
                .get("collection");

        Assert.assertNotNull(fooList, "Should have valid list.");
        Assert.assertEquals(fooList.size(), 3,
                "Filtered list should be the correct size.");
        Assert.assertEquals(fooList.get(2), third,
                "First element should be correct.");
        Assert.assertEquals(fooList.get(1), second,
                "Second element should be correct.");
        Assert.assertNotNull(fooList.get(2),
                "Third element should have been instantiated along the way.");
        Assert.assertTrue(fooList.get(0) instanceof TypesBean,
                "Third element should have be a valid bean.");

        TypesBean first = fooList.get(0);

        Assert.assertEquals(first.getBoolean(), true,
                "Nested value on constructed bean should be corrected.");
    }

    @Test
    public void testAddingIndexed()
    {
        Map<String, Object> rawValues = new HashMap<String, Object>();

        rawValues.put("listID", 1L);
        rawValues.put("boolean", false);
        rawValues.put("collection[].integer", 1);
        rawValues.put("collection[].boolean", true);

        ListBean listBean = ProxyFactory.createAs(ListBean.class, rawValues);

        Assert.assertNotNull(listBean, "Bean instance should be valid.");
        Assert.assertEquals(listBean.getListID(), 1L,
                "Identity should be correct.");
        
        Collection<?> collection = listBean.getCollection();
        
        Assert.assertEquals(collection.size(), 1,
                "List property size should be correct.");

        TypesBean[] typesArray = collection.toArray(new TypesBean[collection.size()]);
        TypesBean first = typesArray[0];

        Assert.assertNotNull(first, "Should have valid first element.");
        Assert.assertEquals(1, first.getInteger(),
                "Should have correct first integer value.");
        Assert.assertEquals(first.getBoolean(), true,
                "Should have correct first boolean value.");

        rawValues.put("collection[].integer", 2);
        rawValues.put("collection[].boolean", false);

        JavaBeanHandler listHandler = ProxyFactory.getHandler(listBean);

        listHandler.propertyValues.putAll(rawValues);

        collection = (Collection<?>) ProxyManipulator.get(
                listBean, "collection");
        
        Assert.assertEquals(collection.size(), 2,
                "List property size should be correct after putAll().");
        
        typesArray = collection.toArray(new TypesBean[collection.size()]);

        first = typesArray[0];

        Assert.assertNotNull(first, "Should still have valid single element.");
        Assert.assertEquals(first.getInteger(), 1,
                "Should have correct updated integer value.");
        Assert.assertEquals(first.getBoolean(), true,
                "Should have correct updated boolean value.");
    }

    @Test
    public void testAddingAnnotated()
    {
        Map<String, Object> rawValues = new HashMap<String, Object>();

        rawValues.put("listID", 1L);
        rawValues.put("boolean", false);
        rawValues.put("annotated[].integer", 1);
        rawValues.put("annotated[].boolean", true);

        ListBean listBean = ProxyFactory.createAs(ListBean.class, rawValues);

        Assert.assertNotNull(listBean, "Bean instance should be valid.");
        Assert.assertEquals(listBean.getListID(), 1L,
                "Identity should be correct.");
        
        Collection<?> collection = listBean.getAnnotated();
        
        Assert.assertEquals(collection.size(), 1,
                "List property size should be correct.");

        TypesBean[] typesArray = collection.toArray(new TypesBean[collection.size()]);
        TypesBean first = typesArray[0];

        Assert.assertNotNull(first, "Should have valid first element.");
        Assert.assertEquals(1, first.getInteger(),
                "Should have correct first integer value.");
        Assert.assertEquals(first.getBoolean(), true,
                "Should have correct first boolean value.");

        rawValues.put("collection[].integer", 2);
        rawValues.put("collection[].boolean", false);

        JavaBeanHandler listHandler = ProxyFactory.getHandler(listBean);

        listHandler.propertyValues.putAll(rawValues);

        collection = listBean.getAnnotated();
        
        Assert.assertEquals(collection.size(), 2,
                "List property size should be correct after putAll().");
        
        typesArray = collection.toArray(new TypesBean[collection.size()]);

        first = typesArray[0];

        Assert.assertNotNull(first, "Should still have valid single element.");
        Assert.assertEquals(first.getInteger(), 1,
                "Should have correct updated integer value.");
        Assert.assertEquals(first.getBoolean(), true,
                "Should have correct updated boolean value.");
    }

    @Test
    public void testArray()
    {
        Map<String, Object> rawValues = new HashMap<String, Object>();

        rawValues.put("listID", 1L);
        rawValues.put("boolean", false);
        rawValues.put("array", new TypesBean[2]);
        rawValues.put("array[0].integer", 1);
        rawValues.put("array[0].boolean", true);

        ListBean listBean = ProxyFactory.createAs(ListBean.class, rawValues);

        Assert.assertNotNull(listBean, "Bean instance should be valid.");
        Assert.assertEquals(listBean.getListID(), 1L,
                "Identity should be correct.");

        TypesBean[] typesArray = listBean.getArray();
        TypesBean first = typesArray[0];
        
        Assert.assertEquals(typesArray.length, 2,
                "List property size should be correct.");
        
        Assert.assertNull(typesArray[1], "Second element should not be set, yet.");

        Assert.assertNotNull(first, "Should have valid first element.");
        Assert.assertEquals(1, first.getInteger(),
                "Should have correct first integer value.");
        Assert.assertEquals(first.getBoolean(), true,
                "Should have correct first boolean value.");
        
        rawValues.clear();

        rawValues.put("array[1].integer", 2);
        rawValues.put("array[1].boolean", false);
        
        JavaBeanHandler listHandler = ProxyFactory.getHandler(listBean);

        listHandler.propertyValues.putAll(rawValues);

        typesArray = listBean.getArray();
        first = typesArray[0];
        
        Assert.assertEquals(typesArray.length, 2,
                "List property size should be correct after putAll().");
        
        Assert.assertNotNull(typesArray[1], "Second element should be set, now.");

        Assert.assertNotNull(first, "Should still have valid single element.");
        Assert.assertEquals(first.getInteger(), 1,
                "Should have correct updated integer value.");
        Assert.assertEquals(first.getBoolean(), true,
                "Should have correct updated boolean value.");

        Assert.assertEquals(typesArray[1].getInteger(), 2,
                "Should have correct second integer value.");
        Assert.assertEquals(typesArray[1].getBoolean(), false,
                "Should have correct second boolean value.");
        
        Assert.assertNotNull(listBean.getArray(0), "Static access of first should work.");
        Assert.assertNotNull(listBean.getArray(1), "Static access of second should work.");
    }

    @Test
    public void testBad()
    {
        Map<String, Object> values = new HashMap<String, Object>();

        values.put("foo", 1);
        values.put("bar", 2);

        try
        {
            ProxyFactory.createAs(ListBean.class, values);
            Assert
                    .fail("List handling should not affect failing against bad property names.");
        }
        catch (Exception e)
        {
            LOGGER.error(e);

            // success
        }

    }
}
