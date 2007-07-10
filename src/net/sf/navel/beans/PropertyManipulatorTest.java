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

import java.util.Arrays;

import net.sf.navel.example.IndexedBean;
import net.sf.navel.example.NestedBean;
import net.sf.navel.example.TypesBean;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 
 * @author cmdln
 * 
 */
public class PropertyManipulatorTest
{

    private static final Logger LOGGER = LogManager
            .getLogger(PropertyManipulatorTest.class);

    @Test
    public void testIndexedProperty()
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class);

        indexedBean.setArray(new String[2]);
        indexedBean.setFloats(new float[2]);
        indexedBean.setTypes(new TypesBean[2]);

        indexedBean.setTypes(0, typesBean);

        PropertyManipulator.put(indexedBean, "array[0]", "foo");
        PropertyManipulator.put(indexedBean, "array[1]", "bar");
        PropertyManipulator.put(indexedBean, "floats[0]", new Float(32.0));
        PropertyManipulator.put(indexedBean, "floats[1]", new Float(64.0));
        PropertyManipulator.put(indexedBean, "types[0].boolean", Boolean.TRUE);
        PropertyManipulator.put(indexedBean, "types[1].boolean", Boolean.FALSE);

        Assert.assertEquals(indexedBean.getArray(0), "foo",
                "First String element should be set correctly.");
        Assert.assertEquals(indexedBean.getArray(1), "bar",
                "Second String element should be set correctly.");

        Assert.assertEquals(indexedBean.getFloats(0), 32.0, 0,
                "First float element should be set correctly.");
        Assert.assertEquals(indexedBean.getFloats(1), 64.0, 0,
                "Second float element should be set correctly.");

        Assert
                .assertEquals(indexedBean.getTypes(0).getBoolean(), true,
                        "Boolean property of first types element should be set correctly.");
        Assert
                .assertEquals(indexedBean.getTypes(1).getBoolean(), false,
                        "Boolean property of second types element should be set correctly.");
    }

    @Test
    public void testIsPropertyOf()
    {
        IndexedBean bean = ProxyFactory.createAs(IndexedBean.class);

        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        Assert.assertTrue(handler.propertyValues.isPropertyOf("types"),
                "Types array should be a valid property.");
        Assert.assertTrue(handler.propertyValues.isPropertyOf("types[0]"),
                "Types array element should be a valid property.");
        Assert.assertTrue(handler.propertyValues
                .isPropertyOf("types[0].boolean"),
                "Types array element nested should be a valid property.");

        Assert.assertFalse(handler.propertyValues.isPropertyOf("foo"));
        Assert.assertFalse(handler.propertyValues.isPropertyOf("foo[0]"));
        Assert.assertFalse(handler.propertyValues.isPropertyOf("foo[0].bar"));

        NestedBean nested = ProxyFactory.createAs(NestedBean.class);

        handler = ProxyFactory.getHandler(nested);

        Assert.assertTrue(
                handler.propertyValues.isPropertyOf("nested.boolean"),
                "Nested property should pass.");

        Assert.assertFalse(handler.propertyValues.isPropertyOf("foo"));
        Assert.assertFalse(handler.propertyValues.isPropertyOf("nested.foo"));
    }

    @Test
    public void testIsSet()
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        Assert.assertFalse(PropertyManipulator.isSet(indexedBean, "types"),
                "Types array should not yet be set.");
        Assert.assertFalse(PropertyManipulator.isSet(indexedBean, "types[0]"),
                "First type element should not yet be set.");
        Assert.assertFalse(PropertyManipulator.isSet(indexedBean,
                "types[0].boolean"),
                "Nested property of first type element should not yet be set.");

        indexedBean.setTypes(new TypesBean[1]);

        Assert.assertTrue(PropertyManipulator.isSet(indexedBean, "types"),
                "Types array should now be set.");
        Assert.assertFalse(PropertyManipulator.isSet(indexedBean, "types[0]"),
                "First type element should not yet be set.");
        Assert.assertFalse(PropertyManipulator.isSet(indexedBean,
                "types[0].boolean"),
                "Nested property of first type element should not yet be set.");

        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class);

        indexedBean.setTypes(0, typesBean);

        Assert.assertTrue(PropertyManipulator.isSet(indexedBean, "types[0]"),
                "First type element should now be set.");
        Assert.assertFalse(PropertyManipulator.isSet(indexedBean,
                "types[0].boolean"),
                "Nested property of first type element should not yet be set.");

        try
        {
            PropertyManipulator.isSet(indexedBean, "foo");

            Assert.fail("Should not be able to reference bad property, foo.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            PropertyManipulator.isSet(indexedBean, "foo[0]");

            Assert
                    .fail("Should not be able to reference bad property, foo[0].");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            PropertyManipulator.isSet(indexedBean, "foo[0].bar");

            Assert
                    .fail("Should not be able to reference bad property, foo[0].bar.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        NestedBean nested = ProxyFactory.createAs(NestedBean.class);

        Assert.assertFalse(PropertyManipulator.isSet(nested, "nested.boolean"),
                "Nested property should not yet be set.");

        nested.setNested(ProxyFactory.createAs(TypesBean.class));
        nested.getNested().setBoolean(true);

        Assert.assertTrue(PropertyManipulator.isSet(nested, "nested.boolean"),
                "Nested property should now be set.");
    }

    @Test
    public void testGet()
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        Assert.assertNull(PropertyManipulator.get(indexedBean, "types"),
                "Types should come back as null.");

        indexedBean.setTypes(new TypesBean[1]);

        Assert.assertTrue(Arrays.deepEquals((TypesBean[]) PropertyManipulator
                .get(indexedBean, "types"), new TypesBean[1]),
                "Should get empty array.");

        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class);

        indexedBean.setTypes(0, typesBean);

        Assert.assertEquals(PropertyManipulator.get(indexedBean, "types[0]"),
                typesBean, "Should get types bean at index.");
        Assert.assertEquals(PropertyManipulator.get(indexedBean,
                "types[0].boolean"), false, "Should get uninitialized value.");

        typesBean.setBoolean(true);

        Assert.assertEquals(PropertyManipulator.get(indexedBean,
                "types[0].boolean"), true, "Should get set value.");

        try
        {
            PropertyManipulator.get(indexedBean, "foo");

            Assert.fail("Should not be able to reference bad property, foo.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            PropertyManipulator.get(indexedBean, "foo[0]");

            Assert
                    .fail("Should not be able to reference bad property, foo[0].");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            PropertyManipulator.get(indexedBean, "foo[0].bar");

            Assert
                    .fail("Should not be able to reference bad property, foo[0].bar.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        NestedBean nested = ProxyFactory.createAs(NestedBean.class);

        PropertyManipulator.clear(typesBean, "boolean");
        nested.setNested(typesBean);

        Assert.assertEquals(PropertyManipulator.get(nested, "nested.boolean"),
                false, "Nested boolean should have uninitialized value.");

        nested.getNested().setBoolean(true);

        Assert.assertEquals(PropertyManipulator.get(nested, "nested.boolean"),
                true, "Nested boolean should have set value.");
    }

}
