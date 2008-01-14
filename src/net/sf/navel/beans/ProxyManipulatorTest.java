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
import java.util.Map;
import java.util.Map.Entry;

import net.sf.navel.example.AncestorBean;
import net.sf.navel.example.CharacterAsStringDelegate;
import net.sf.navel.example.IndexedBean;
import net.sf.navel.example.NestedBean;
import net.sf.navel.example.StringBean;
import net.sf.navel.example.TypesBean;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * 
 * @author cmdln
 * 
 */
public class ProxyManipulatorTest
{

    private static final Logger LOGGER = LogManager
            .getLogger(ProxyManipulatorTest.class);

    @Test
    public void testIndexedProperty()
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class);

        indexedBean.setArray(new String[2]);
        indexedBean.setFloats(new float[2]);
        indexedBean.setTypes(new TypesBean[2]);

        indexedBean.setTypes(0, typesBean);

        ProxyManipulator.put(indexedBean, "array[0]", "foo");
        ProxyManipulator.put(indexedBean, "array[1]", "bar");
        ProxyManipulator.put(indexedBean, "floats[0]", new Float(32.0));
        ProxyManipulator.put(indexedBean, "floats[1]", new Float(64.0));
        ProxyManipulator.put(indexedBean, "types[0].boolean", Boolean.TRUE);
        ProxyManipulator.put(indexedBean, "types[1].boolean", Boolean.FALSE);

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

        nested.setNested(ProxyFactory.createAs(TypesBean.class));

        Assert.assertTrue(
                handler.propertyValues.isPropertyOf("nested.boolean"),
                "Nested property after empty set should pass.");

        nested.getNested().setBoolean(true);

        Assert.assertTrue(
                handler.propertyValues.isPropertyOf("nested.boolean"),
                "Nested property after full set should pass.");

        Assert.assertFalse(handler.propertyValues.isPropertyOf("foo"));
        Assert.assertFalse(handler.propertyValues.isPropertyOf("nested.foo"));
    }

    @Test
    public void testIsSet()
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        Assert.assertFalse(ProxyManipulator.isSet(indexedBean, "types"),
                "Types array should not yet be set.");
        Assert.assertFalse(ProxyManipulator.isSet(indexedBean, "types[0]"),
                "First type element should not yet be set.");
        Assert.assertFalse(ProxyManipulator.isSet(indexedBean,
                "types[0].boolean"),
                "Nested property of first type element should not yet be set.");

        indexedBean.setTypes(new TypesBean[1]);

        Assert.assertTrue(ProxyManipulator.isSet(indexedBean, "types"),
                "Types array should now be set.");
        Assert.assertFalse(ProxyManipulator.isSet(indexedBean, "types[0]"),
                "First type element should not yet be set.");
        Assert.assertFalse(ProxyManipulator.isSet(indexedBean,
                "types[0].boolean"),
                "Nested property of first type element should not yet be set.");

        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class);

        indexedBean.setTypes(0, typesBean);

        Assert.assertTrue(ProxyManipulator.isSet(indexedBean, "types[0]"),
                "First type element should now be set.");
        Assert.assertFalse(ProxyManipulator.isSet(indexedBean,
                "types[0].boolean"),
                "Nested property of first type element should not yet be set.");

        try
        {
            ProxyManipulator.isSet(indexedBean, "foo");

            Assert.fail("Should not be able to reference bad property, foo.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.isSet(indexedBean, "foo[0]");

            Assert
                    .fail("Should not be able to reference bad property, foo[0].");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.isSet(indexedBean, "foo[0].bar");

            Assert
                    .fail("Should not be able to reference bad property, foo[0].bar.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        NestedBean nested = ProxyFactory.createAs(NestedBean.class);

        Assert.assertFalse(ProxyManipulator.isSet(nested, "nested.boolean"),
                "Nested property should not yet be set.");

        nested.setNested(ProxyFactory.createAs(TypesBean.class));
        nested.getNested().setBoolean(true);

        Assert.assertTrue(ProxyManipulator.isSet(nested, "nested.boolean"),
                "Nested property should now be set.");
    }

    @Test
    public void testGet()
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        Assert.assertNull(ProxyManipulator.get(indexedBean, "types"),
                "Types should come back as null.");

        indexedBean.setTypes(new TypesBean[1]);

        Assert.assertTrue(Arrays.deepEquals((TypesBean[]) ProxyManipulator
                .get(indexedBean, "types"), new TypesBean[1]),
                "Should get empty array.");

        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class);

        indexedBean.setTypes(0, typesBean);

        Assert.assertEquals(ProxyManipulator.get(indexedBean, "types[0]"),
                typesBean, "Should get types bean at index.");
        Assert.assertNull(ProxyManipulator.get(indexedBean,
                "types[0].boolean"), "Should get null value.");

        typesBean.setBoolean(true);

        Assert.assertEquals(ProxyManipulator.get(indexedBean,
                "types[0].boolean"), true, "Should get set value.");

        try
        {
            ProxyManipulator.get(indexedBean, "foo");

            Assert.fail("Should not be able to reference bad property, foo.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.get(indexedBean, "foo[0]");

            Assert
                    .fail("Should not be able to reference bad property, foo[0].");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.get(indexedBean, "foo[0].bar");

            Assert
                    .fail("Should not be able to reference bad property, foo[0].bar.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        NestedBean nested = ProxyFactory.createAs(NestedBean.class);
        
        ProxyManipulator.clear(nested, "nested");
        
        Assert.assertNull(nested.getNested(), "Nested bean should not be set.");
        Assert.assertNull(ProxyManipulator.get(nested, "nested.boolean"),
                "De-referencing null should return null.");

        ProxyManipulator.clear(typesBean, "boolean");
        nested.setNested(typesBean);

        Assert.assertNull(ProxyManipulator.get(nested, "nested.boolean"),
                "Nested boolean should have uninitialized value.");
        nested.getNested().setBoolean(true);

        Assert.assertEquals(ProxyManipulator.get(nested, "nested.boolean"),
                true, "Nested boolean should have set value.");
    }

    @Test(dataProvider = "copyAll")
    public void testCopyAll(int expectedSize, boolean flatten)
    {
        NestedBean bean = ProxyFactory.createAs(NestedBean.class);
        bean.setNested(ProxyFactory.createAs(TypesBean.class));

        bean.getNested().setBoolean(true);
        bean.getNested().setCharacter('q');
        bean.getNested().setInteger(100);

        Map<String, Object> values = ProxyManipulator.copyAll(bean, flatten);

        Assert.assertEquals(values.size(), expectedSize, String.format(
                "Should have had correct size of values Map for flatten, %s.",
                flatten));

        if (flatten)
        {
            for (Entry<String, Object> entry : values.entrySet())
            {
                Assert.assertNull(ProxyFactory.getHandler(entry.getValue()),
                        String.format(
                                "Property, %s, should not have been a bean!",
                                entry.getKey()));
            }
        }
        else
        {
            Assert.assertEquals(values.get("nested"), bean.getNested(),
                    "Should have fully populated bean still in the map.");
        }
    }

    @Test
    public void testDeepNesting()
    {
        AncestorBean ancestor = ProxyFactory.createAs(AncestorBean.class);

        ancestor.setName("foo bar baz");

        ancestor.setChild(ProxyFactory.createAs(NestedBean.class));

        ancestor.getChild().setBoolean(false);
        ancestor.getChild().setCharacter('z');
        ancestor.getChild().setInteger(99);

        ancestor.getChild().setNested(ProxyFactory.createAs(TypesBean.class));

        ancestor.getChild().getNested().setBoolean(true);
        ancestor.getChild().getNested().setCharacter('q');
        ancestor.getChild().getNested().setInteger(100);

        Map<String, Object> values = ProxyManipulator
                .copyAll(ancestor, true);

        Assert.assertEquals(values.size(), 7,
                "Should have fully flattened out.");
        Assert.assertEquals(values.get("name"), "foo bar baz");
        Assert.assertEquals(values.get("child.boolean"), Boolean.FALSE);
        Assert.assertEquals(values.get("child.character"), Character
                .valueOf('z'));
        Assert.assertEquals(values.get("child.integer"), Integer.valueOf(99));
        Assert.assertEquals(values.get("child.nested.boolean"), Boolean.TRUE);
        Assert.assertEquals(values.get("child.nested.character"), Character
                .valueOf('q'));
        Assert.assertEquals(values.get("child.nested.integer"), Integer
                .valueOf(100));
    }
    
    @Test
    public void testResolveAll()
    {
        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class,
                StringBean.class);

        ProxyFactory.attach(typesBean, "string",
                new CharacterAsStringDelegate());

        Assert.assertTrue(
                ProxyFactory.isAttached(typesBean, "string"),
                "Should spot the property delegate.");

        typesBean.setCharacter('a');
        
        Map<String,Object> values = ProxyManipulator.copyAll(typesBean);
        
        Assert.assertEquals(values.size(), 1, "Internal storage should only have one entry.");
        Assert.assertEquals(values.get("character"), (Character) 'a', "Internal storage should be correct.");
        
        values = ProxyManipulator.resolveAll(typesBean);
        
        Assert.assertEquals(values.size(), 1, "Delegate values should only have one entry.");
        Assert.assertEquals(values.get("string"), "a", "Delegated values should be correct.");
    }
    
    @Test
    public void testResolveAllNested()
    {
        NestedBean nested = ProxyFactory.createAs(NestedBean.class);
        
        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class,
                StringBean.class);
        
        nested.setNested(typesBean);

        ProxyFactory.attach(typesBean, "string",
                new CharacterAsStringDelegate());

        Assert.assertTrue(
                ProxyFactory.isAttached(typesBean, "string"),
                "Should spot the property delegate.");

        typesBean.setCharacter('a');
        
        Map<String,Object> values = ProxyManipulator.copyAll(nested, true);
        
        Assert.assertEquals(values.size(), 1, "Internal storage should only have one entry.");
        Assert.assertTrue(values.containsKey("nested.character"), "Nested property value should have been prefixed.");
        Assert.assertEquals(values.get("nested.character"), (Character) 'a', "Internal storage should be correct.");
        
        values = ProxyManipulator.resolveAll(nested, true);
        
        Assert.assertEquals(values.size(), 1, "Delegate values should only have one entry.");
        Assert.assertTrue(values.containsKey("nested.string"), "Nested property delegate should have been prefixed.");
        Assert.assertEquals(values.get("nested.string"), "a", "Delegated values should be correct.");
        
        values = ProxyManipulator.resolveAll(nested, false);
        
        Assert.assertTrue(values.isEmpty(), "Delegate values should be empty on shallow resolve.");
        Assert.assertFalse(values.containsKey("nested.string"), "Nested property delegate should not be present.");
    }
    
    @Test
    public void testResolve()
    {
        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class,
                StringBean.class);

        ProxyFactory.attach(typesBean, "string",
                new CharacterAsStringDelegate());

        Assert.assertTrue(
                ProxyFactory.isAttached(typesBean, "string"),
                "Should spot the property delegate.");

        typesBean.setCharacter('a');
        
        Map<String,Object> values = ProxyManipulator.copyAll(typesBean);
        
        Assert.assertEquals(values.size(), 1, "Internal storage should only have one entry.");
        Assert.assertEquals(values.get("character"), (Character) 'a', "Internal storage should be correct.");
        
        Object value = ProxyManipulator.resolve(typesBean, "string");
        
        Assert.assertNotNull(value, "Delegate value should be valid.");
        Assert.assertEquals(value, "a", "Delegated value should be correct.");
    }
    
    @Test
    public void testResolveNested()
    {
        NestedBean nested = ProxyFactory.createAs(NestedBean.class);
        
        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class,
                StringBean.class);
        
        nested.setNested(typesBean);

        ProxyFactory.attach(typesBean, "string",
                new CharacterAsStringDelegate());

        Assert.assertTrue(
                ProxyFactory.isAttached(typesBean, "string"),
                "Should spot the property delegate.");
        Assert.assertTrue(
                ProxyFactory.isAttached(nested, "nested.string"),
                "Should spot the property delegate, using dot notation.");

        // also ensure using a dot-notation expression will work
        ProxyFactory.attach(nested, "nested.string",
                new CharacterAsStringDelegate());

        Assert.assertTrue(
                ProxyFactory.isAttached(typesBean, "string"),
                "Should spot the property delegate.");
        Assert.assertTrue(
                ProxyFactory.isAttached(nested, "nested.string"),
                "Should spot the property delegate, using dot notation.");

        typesBean.setCharacter('a');
        
        Map<String,Object> values = ProxyManipulator.copyAll(nested, true);
        
        Assert.assertEquals(values.size(), 1, "Internal storage should only have one entry.");
        Assert.assertTrue(values.containsKey("nested.character"), "Nested property value should have been prefixed.");
        Assert.assertEquals(values.get("nested.character"), (Character) 'a', "Internal storage should be correct.");
        
        Object value = ProxyManipulator.resolve(nested, "nested.string");
        
        Assert.assertNotNull(value, "Delegate values should be valid.");
        Assert.assertEquals(value, "a", "Delegated value should be correct.");
        
        value = ProxyManipulator.resolve(nested, "string");
        
        Assert.assertNull(value, "Delegate value should be invalid on shallow resolve.");
    }
    
    @Test
    public void testClearNested()
    {
        NestedBean nested = ProxyFactory.createAs(NestedBean.class);
        
        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class,
                StringBean.class);
        
        nested.setNested(typesBean);

        typesBean.setCharacter('a');
        
        Map<String,Object> values = ProxyManipulator.copyAll(nested, true);
        
        Assert.assertEquals(values.size(), 1, "Internal storage should only have one entry.");
        Assert.assertTrue(values.containsKey("nested.character"), "Nested property value should have been prefixed.");
        Assert.assertEquals(values.get("nested.character"), (Character) 'a', "Internal storage should be correct.");
        
        boolean removed = ProxyManipulator.clear(nested, "nested.character");
        
        Assert.assertTrue(removed, "Should have indicated a successful remove.");
        
        removed= ProxyManipulator.clear(nested, "character");
        
        Assert.assertFalse(removed, "Should not have indicated a successful remove.");
    }


    @DataProvider(name = "copyAll")
    public Object[][] createData()
    {
        return new Object[][]
        { new Object[]
        { 1, false }, new Object[]
        { 3, true } };
    }
}
