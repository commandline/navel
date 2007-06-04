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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import junit.framework.TestCase;
import net.sf.navel.beans.PropertyBeanHandler;
import net.sf.navel.example.ListBean;
import net.sf.navel.example.TypesBean;

/**
 * @author thomas
 * 
 */
public class ListBuilderTest extends TestCase
{

    private static final Logger LOGGER = LogManager
            .getLogger(ListBuilderTest.class);

    /*
     * Test method for 'net.sf.navel.beans.ListBuilder.filter(Class<?>, Map<String,
     * Object>)'
     */
    @SuppressWarnings("unchecked")
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

        assertNotNull("Should have valid list.", fooList);
        assertEquals("Filtered list should be the correct size.", 3, fooList
                .size());
        assertEquals("First element should be correct.", third, fooList.get(2));
        assertEquals("Second element should be correct.", second, fooList
                .get(1));
        assertNotNull(
                "Third element should have been instantiated along the way.",
                fooList.get(2));
        assertTrue("Third element should have be a valid bean.",
                fooList.get(0) instanceof TypesBean);

        TypesBean first = fooList.get(0);

        assertEquals("Nested value on constructed bean should be corrected.",
                true, first.getBoolean());
    }

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

        assertNotNull("Bean instance should be valid.", listBean);
        assertEquals("Identity should be correct.", 1L, listBean.getListID());
        assertEquals("List property size should be correct.", 1, listBean
                .getTypesList().size());

        TypesBean first = listBean.getTypesList(0);

        assertNotNull("Should have valid first element.", first);
        assertEquals("Should have correct first integer value.", 1, first
                .getInteger());
        assertEquals("Should have correct first boolean value.", true, first
                .getBoolean());

        rawValues.put("typesList[].integer", 2);
        rawValues.put("typesList[].boolean", false);

        listHandler.putAll(rawValues);

        assertEquals("List property size should be correct after putAll().", 2,
                listBean.getTypesList().size());

        first = listBean.getTypesList(0);

        assertNotNull("Should still have valid first element.", first);
        assertEquals("Should still have correct first integer value.", 1, first
                .getInteger());
        assertEquals("Should still have correct first boolean value.", true,
                first.getBoolean());

        TypesBean second = listBean.getTypesList(1);

        assertNotNull("Should have valid second element.", second);
        assertEquals("Should have correct second integer value.", 2, second
                .getInteger());
        assertEquals("Should have correct second boolean value.", false, second
                .getBoolean());
    }

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

        assertNotNull("Bean instance should be valid.", listBean);
        assertEquals("Identity should be correct.", 1L, listBean.getListID());
        assertEquals("List property size should be correct.", 1, listBean
                .getAnnotated().size());

        TypesBean first = listBean.getAnnotated().get(0);

        assertNotNull("Should have valid first element.", first);
        assertEquals("Should have correct first integer value.", 1, first
                .getInteger());
        assertEquals("Should have correct first boolean value.", true, first
                .getBoolean());
    }

    public void testBad()
    {
        Map<String, Object> values = new HashMap<String, Object>();

        values.put("foo", 1);
        values.put("bar", 2);

        try
        {
            new PropertyBeanHandler<ListBean>(ListBean.class, values, true);
            fail("List handling should not affect failing against bad property names.");
        }
        catch (Exception e)
        {
            LOGGER.error(e);

            // success
        }

    }
}
