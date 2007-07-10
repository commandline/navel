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

import java.util.HashMap;
import java.util.Map;

import net.sf.navel.example.ChildBean;
import net.sf.navel.example.NestedBean;
import net.sf.navel.example.TypesBean;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author cmdln
 * 
 */
public class PropertyValidatorTest
{

    private static final Logger LOGGER = LogManager
            .getLogger(PropertyValidatorTest.class);

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @BeforeMethod
    protected void setUp() throws Exception
    {
        Logger root = LogManager.getRootLogger();

        root.removeAllAppenders();

        root.addAppender(new ConsoleAppender(new PatternLayout(
                "%d %-5p [%c] %m%n"), ConsoleAppender.SYSTEM_OUT));
        root.setLevel(Level.DEBUG);
    }

    /**
     * Test method for
     * {@link net.sf.navel.beans.PropertyValidator#validateData(java.beans.BeanInfo)}.
     */
    @Test
    public void testValid()
    {
        Map<String, Object> values = new HashMap<String, Object>(2);

        values.put("parentID", 1L);
        values.put("childID", 2L);

        ChildBean bean = ProxyFactory.createAs(ChildBean.class, values);

        Assert.assertEquals(bean.getParentID(), 1L,
                "Inherited properties should work.");
        Assert.assertEquals(bean.getChildID(), 2L,
                "Declared properties should work.");

        PropertyManipulator.put(bean, "parentID", 3L);
        PropertyManipulator.put(bean, "childID", 4L);

        Assert.assertEquals(bean.getParentID(), 3L,
                "Inherited properties should work.");
        Assert.assertEquals(bean.getChildID(), 4L,
                "Declared properties should work.");
    }

    @Test
    public void testValidNested()
    {
        TypesBean nested = ProxyFactory.createAs(TypesBean.class);
        nested.setInteger(1);

        Map<String, Object> values = new HashMap<String, Object>(2);

        values.put("long", 2L);
        values.put("nested", nested);

        NestedBean bean = ProxyFactory.createAs(NestedBean.class, values);

        Assert.assertEquals(bean.getLong(), 2L,
                "Simple properties should work.");
        Assert.assertEquals(bean.getNested().getInteger(), 1,
                "Nested properties should work.");

        PropertyManipulator.put(bean, "nested.integer", 3);

        Assert.assertEquals(bean.getNested().getInteger(), 3,
                "Putting nested properties should work.");
    }

    @Test
    public void testBadPropertyName()
    {
        try
        {
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("foo", 1L);

            ProxyFactory.createAs(TypesBean.class, values);

            Assert
                    .fail("Should not be able to initialize with bad property name!");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        TypesBean bean = ProxyFactory.createAs(TypesBean.class);

        try
        {
            PropertyManipulator.put(bean, "foo", 1L);
            Assert
                    .fail("Should not be able to use the PropertyManipulator to set a bad property name, should be consistent with construction check.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }
    }

    @Test
    public void testBadPropertyValue()
    {
        try
        {
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("boolean", 1L);

            ProxyFactory.createAs(TypesBean.class, values);

            Assert
                    .fail("Should not be able to initialize with bad property value!");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        TypesBean bean = ProxyFactory.createAs(TypesBean.class);

        try
        {
            PropertyManipulator.put(bean, "boolean", 1L);
            Assert
                    .fail("Should not be able to use the PropertyManipulator to set a bad property value!");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }
    }

    @Test
    public void testBadNested()
    {
        try
        {
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("nested", 1L);

            ProxyFactory.createAs(NestedBean.class, values);

            Assert
                    .fail("Should not be able to initialize nested with bad property value!");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        NestedBean bean = ProxyFactory.createAs(NestedBean.class);

        try
        {
            PropertyManipulator.put(bean, "nested", 1L);
            Assert
                    .fail("Should not be able to use the PropertyManipulator to set a bad nested property name!");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }
    }
}
