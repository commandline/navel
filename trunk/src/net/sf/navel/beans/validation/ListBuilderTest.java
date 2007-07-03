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
package net.sf.navel.beans.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.navel.beans.PropertyBeanHandler;
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
public class ListBuilderTest
{

    private static final Logger LOGGER = LogManager
            .getLogger(ListBuilderTest.class);

    /*
     * Test method for 'net.sf.navel.beans.ListBuilder.filter(Class<?>, Map<String,
     * Object>)'
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFilter()
    {
        Map<String, Object> rawValues = new TreeMap<String, Object>();

        TypesBean second = new PropertyBeanHandler<TypesBean>(TypesBean.class)
                .getProxy();
        TypesBean third = new PropertyBeanHandler<TypesBean>(TypesBean.class)
                .getProxy();

        rawValues.put("typesList[0].boolean", true);
        rawValues.put("typesList[1]", second);
        rawValues.put("typesList[?]", third);

        ListBuilder builder = new ListBuilder(
                new PropertyBeanHandler<ListBean>(ListBean.class, rawValues,
                        true));

        Map<String, ?> filteredValues = builder.handler.getValues();

        List<TypesBean> fooList = (List<TypesBean>) filteredValues
                .get("typesList");

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
    public void testAdding()
    {
        Map<String, Object> rawValues = new HashMap<String, Object>();

        rawValues.put("listID", 1L);
        rawValues.put("boolean", false);
        rawValues.put("typesList[].integer", 1);
        rawValues.put("typesList[].boolean", true);

        PropertyBeanHandler<ListBean> listHandler = new PropertyBeanHandler<ListBean>(
                ListBean.class, rawValues, true);

        ListBean listBean = listHandler.getProxy();

        Assert.assertNotNull(listBean, "Bean instance should be valid.");
        Assert.assertEquals(listBean.getListID(), 1L,
                "Identity should be correct.");
        Assert.assertEquals(listBean.getTypesList().size(), 1,
                "List property size should be correct.");

        TypesBean first = listBean.getTypesList(0);

        Assert.assertNotNull(first, "Should have valid first element.");
        Assert.assertEquals(1, first.getInteger(),
                "Should have correct first integer value.");
        Assert.assertEquals(first.getBoolean(), true,
                "Should have correct first boolean value.");

        rawValues.put("typesList[].integer", 2);
        rawValues.put("typesList[].boolean", false);

        listHandler.putAll(rawValues);

        Assert.assertEquals(listBean.getTypesList().size(), 2,
                "List property size should be correct after putAll().");

        first = listBean.getTypesList(0);

        Assert.assertNotNull(first, "Should still have valid first element.");
        Assert.assertEquals(first.getInteger(), 1,
                "Should still have correct first integer value.");
        Assert.assertEquals(first.getBoolean(), true,
                "Should still have correct first boolean value.");

        TypesBean second = listBean.getTypesList(1);

        Assert.assertNotNull(second, "Should have valid second element.");
        Assert.assertEquals(second.getInteger(), 2,
                "Should have correct second integer value.");
        Assert.assertEquals(second.getBoolean(), false,
                "Should have correct second boolean value.");
    }

    @Test
    public void testAnnotated()
    {
        Map<String, Object> rawValues = new HashMap<String, Object>();

        rawValues.put("listID", 1L);
        rawValues.put("boolean", false);
        rawValues.put("annotated[].integer", 1);
        rawValues.put("annotated[].boolean", true);

        PropertyBeanHandler<ListBean> listHandler = new PropertyBeanHandler<ListBean>(
                ListBean.class, rawValues, true);

        ListBean listBean = listHandler.getProxy();

        Assert.assertNotNull(listBean, "Bean instance should be valid.");
        Assert.assertEquals(listBean.getListID(), 1L,
                "Identity should be correct.");
        Assert.assertEquals(listBean.getAnnotated().size(), 1,
                "List property size should be correct.");

        TypesBean first = listBean.getAnnotated().get(0);

        Assert.assertNotNull(first, "Should have valid first element.");
        Assert.assertEquals(first.getInteger(), 1,
                "Should have correct first integer value.");
        Assert.assertEquals(first.getBoolean(), true,
                "Should have correct first boolean value.");
    }

    @Test
    public void testBad()
    {
        Map<String, Object> values = new HashMap<String, Object>();

        values.put("foo", 1);
        values.put("bar", 2);

        try
        {
            new PropertyBeanHandler<ListBean>(ListBean.class, values, true);
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
