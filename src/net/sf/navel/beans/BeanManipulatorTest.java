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

import static net.sf.navel.beans.BeanManipulator.describe;
import static net.sf.navel.beans.BeanManipulator.populate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sf.navel.example.BadBeanImpl;
import net.sf.navel.example.IndexedBean;
import net.sf.navel.example.NestedBean;
import net.sf.navel.example.PropertyNames;
import net.sf.navel.example.ReadWriteBean;
import net.sf.navel.example.SourceBean;
import net.sf.navel.example.TargetBean;
import net.sf.navel.example.TypesBean;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the two primary activities of the bean manipulator.
 * 
 * @author cmdln
 */
public class BeanManipulatorTest
{
    private static final Logger LOGGER = Logger
            .getLogger(BeanManipulatorTest.class);

    /**
     * Exercise bean description on the ReadWriteBean backed by the
     * ProperyBeanHandler.
     * 
     * @throws InvalidPropertyValueException
     *             Not testing construction, error on problem.
     * @throws UnsupportedFeatureException
     *             Not testing construction, error on problem.
     */
    @Test
    public void testDescribeReadWrite() throws InvalidPropertyValueException,
            UnsupportedFeatureException
    {
        Map<String, Object> values = new HashMap<String, Object>(3);

        values.put(PropertyNames.RO_PROP, 1);
        values.put(PropertyNames.WO_PROP, 2);
        values.put(PropertyNames.RW_PROP, 3);

        ReadWriteBean bean = ProxyFactory.createAs(ReadWriteBean.class, values);

        values.remove(PropertyNames.WO_PROP);

        Map<String, Object> descriptions = describe(bean);

        Assert.assertEquals(values, descriptions,
                "Description should match initial values.");
    }

    /**
     * Test that BeanManipulator doesn't return a null Map from describe when
     * there is nothing to describe.
     */
    @Test
    public void testNoProperties()
    {
        BadBeanImpl bean = new BadBeanImpl();

        Map<String, Object> values = describe(bean);

        Assert.assertNotNull(values, "Describe should never return null.");
        Assert.assertTrue(values.isEmpty(),
                "Describe should return empty map when there's a problem.");
    }

    /**
     * Test populating the ReadWriteBean backed by a PropertyBeanHandler with
     * the BeanManipulator.
     * 
     * @throws UnsupportedFeatureException
     *             Not testing construction, error on problem.
     * @throws InvalidPropertyValueException
     *             Not testing construction, error on problem.
     */
    @Test
    public void testPopulateReadWrite() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        Map<String, Object> values = new HashMap<String, Object>(3);
        values.put(PropertyNames.RO_PROP, 1);
        values.put(PropertyNames.WO_PROP, 2);
        values.put(PropertyNames.RW_PROP, 3);

        ReadWriteBean bean = ProxyFactory.createAs(ReadWriteBean.class, values);

        populate(bean, values);

        Assert.assertEquals(bean.getReadOnly(), 1,
                "Bean read only should be set.");
        values = ProxyFactory.getHandler(bean).propertyValues.copyValues(false);
        LOGGER.debug(values);
        Assert.assertNotNull(values.get(PropertyNames.WO_PROP),
                "Bean write only should be set.");
        Assert.assertEquals(values.get(PropertyNames.WO_PROP), Integer
                .valueOf(2), "Bean write only should be set correctly.");
        Assert.assertEquals(3, bean.getReadWrite(),
                "bean read/write should be set.");
    }

    /**
     * Test extracting from and inject into concrete beans.
     */
    @Test
    public void testFromBeanToBean()
    {
        byte fooSource = (byte) 64;
        short barSource = (short) 128;
        int bazSource = 256;
        float quuxSource = 1.204f;

        boolean fooTarget = false;
        short barTarget = (short) 129;
        int bazTarget = 257;
        double quuxTarget = 2.048d;

        SourceBean sourceBean = new SourceBean();

        sourceBean.setFoo(fooSource);
        sourceBean.setBar(barSource);
        sourceBean.setBaz(bazSource);
        sourceBean.setQuux(quuxSource);

        Map<String, Object> values = describe(sourceBean);

        Assert.assertNotNull(values, "Extracted values should be set.");
        Assert.assertEquals(values.get("foo"), Byte.valueOf(fooSource),
                "Check source foo.");
        Assert.assertEquals(values.get("bar"), Short.valueOf(barSource),
                "Check source bar.");
        Assert.assertEquals(values.get("baz"), Integer.valueOf(bazSource),
                "Check source baz.");
        Assert.assertEquals(values.get("quux"), Float.valueOf(quuxSource),
                "Check source quux.");

        TargetBean targetBean = new TargetBean();

        targetBean.setFoo(fooTarget);
        targetBean.setBar(barTarget);
        targetBean.setBaz(bazTarget);
        targetBean.setQuux(quuxTarget);

        populate(targetBean, values);

        Assert
                .assertEquals(fooTarget, targetBean.getFoo(),
                        "Check target foo.");
        Assert
                .assertEquals(barSource, targetBean.getBar(),
                        "Check target bar.");
        Assert
                .assertEquals(bazSource, targetBean.getBaz(),
                        "Check target baz.");
        Assert.assertEquals(quuxTarget, targetBean.getQuux(), 0.0d,
                "Check target quux.");
    }

    /**
     * Test population of nested properties, using "dot notation".
     * 
     * @throws InvalidPropertyValueException
     *             Ignore construction errors.
     * @throws UnsupportedFeatureException
     *             Ignore construction errors.
     */
    @Test
    public void testPopulateNested() throws InvalidPropertyValueException,
            UnsupportedFeatureException
    {
        TypesBean nestedBean = ProxyFactory.createAs(TypesBean.class);

        Map<String, Object> values = new HashMap<String, Object>(2);

        values.put("nested", nestedBean);

        NestedBean bean = ProxyFactory.createAs(NestedBean.class, values);

        Assert.assertNotNull(bean.getNested(),
                "Nested bean should not be null.");

        values.clear();

        // test convert values from a String to a Boolean
        values.put("nested.boolean", "true");
        values.put("nested.long", Long.valueOf(128));

        populate(bean, values);

        Assert.assertEquals(true, bean.getNested().getBoolean(),
                "Nested boolean should be set correctly.");
        Assert.assertEquals(128L, bean.getNested().getLong(),
                "Nested long should be set correctly.");

        LOGGER.debug(values);
        LOGGER.debug(ProxyFactory.getHandler(bean).propertyValues
                .copyValues(false));
    }

    /**
     * Test population of indexed properties.
     * 
     * @throws InvalidPropertyValueException
     *             Ignore construction errors.
     * @throws UnsupportedFeatureException
     *             Ignore construction errors.
     */
    @Test
    public void testPopulateIndexed() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class);

        indexedBean.setArray(new String[2]);
        indexedBean.setFloats(new float[2]);
        indexedBean.setTypes(new TypesBean[]
        { typesBean });

        Map<String, Object> values = new HashMap<String, Object>(5);

        values.put("array[0]", "foo");
        values.put("array[1]", "bar");
        values.put("floats[0]", new Float(32.0));
        values.put("floats[1]", new Float(64.0));
        values.put("types[0].boolean", Boolean.TRUE);

        populate(indexedBean, values);

        Assert.assertEquals("foo", indexedBean.getArray(0),
                "First String element should be set correctly.");
        Assert.assertEquals("bar", indexedBean.getArray(1),
                "Second String element should be set correctly.");

        Assert.assertEquals(32.0, indexedBean.getFloats(0), 0,
                "First float element should be set correctly.");
        Assert.assertEquals(64.0, indexedBean.getFloats(1), 0,
                "Second float element should be set correctly.");

        Assert
                .assertEquals(true, indexedBean.getTypes(0).getBoolean(),
                        "Boolean property of first types element should be set correctly.");
    }

    @Test
    public void testPut()
    {
        SourceBean bean = new SourceBean();
        bean.setArray(new String[3]);
        
        BeanManipulator.putValue(bean, "array[0]", "foo");
        BeanManipulator.putValue(bean, "array[1]", "bar");
        BeanManipulator.putValue(bean, "array[2]", "baz");

        Assert.assertEquals(bean.getArray(), new String[] { "foo", "bar", "baz" },
                "Nested boolean should be set correctly.");
    }

    @Test
    public void testPutNested()
    {
        NestedBean bean = ProxyFactory.createAs(NestedBean.class);
        bean.setNested(ProxyFactory.createAs(TypesBean.class));

        // test convert values from a String to a Boolean
        BeanManipulator.putValue(bean, "nested.boolean", "true");
        BeanManipulator.putValue(bean, "nested.long", Long.valueOf(128));

        Assert.assertEquals(true, bean.getNested().getBoolean(),
                "Nested boolean should be set correctly.");
        Assert.assertEquals(128L, bean.getNested().getLong(),
                "Nested long should be set correctly.");

        LOGGER.debug(ProxyFactory.getHandler(bean).propertyValues
                .copyValues(false));
    }
    
    @Test
    public void testPutIndexed()
    {
        IndexedBean bean = ProxyFactory.createAs(IndexedBean.class);

        BeanManipulator.putValue(bean, "array", new String[] { "foo", "bar" });
        
        Assert.assertEquals(bean.getArray().length, 2);
        Assert.assertEquals(bean.getArray()[0], "foo");
        Assert.assertEquals(bean.getArray()[1], "bar");
        
        BeanManipulator.putValue(bean, "array[1]", "baz");
        
        Assert.assertEquals(bean.getArray()[1], "baz");
        
        Object array = BeanManipulator.resolveValue("array", BeanManipulator.describe(bean));
        
        assert Arrays.deepEquals((String[]) array, new String[] { "foo", "baz"});
    }

    /**
     * Test population of indexed properties with a bad value for the index
     * inside the braces.
     * 
     * @throws InvalidPropertyValueException
     *             Ignore construction errors.
     * @throws UnsupportedFeatureException
     *             Ignore construction errors.
     */
    @Test
    public void testBadIndexed() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        indexedBean.setArray(new String[2]);
        indexedBean.setFloats(new float[2]);
        indexedBean.setTypes(new TypesBean[1]);

        Map<String, Object> values = new HashMap<String, Object>(5);

        values.put("array[false]", "foo");
        values.put("array[1.0d]", "bar");
        values.put("floats[foo]", new Float(32.0));
        values.put("floats[1.0d]", new Float(64.0));
        values.put("types[bar].boolean", Boolean.TRUE);

        populate(indexedBean, values, true);

        Assert.assertNull(indexedBean.getArray(0),
                "First String element should not be set.");
        Assert.assertNull(indexedBean.getArray(1),
                "Second String element should not be set.");

        Assert.assertEquals(0.0, indexedBean.getFloats(0), 0,
                "First float element should not be set.");
        Assert.assertEquals(0.0, indexedBean.getFloats(1), 0,
                "Second float element should not be set.");

        Assert.assertNull(indexedBean.getTypes(0),
                "First types element should not be set");
    }

    /**
     * Test that String to primitive conversion works as expected.
     * 
     * @throws InvalidPropertyValueException
     *             Ignore construction errors.
     * @throws UnsupportedFeatureException
     *             Ignore construction errors.
     */
    @Test
    public void testStringConversion() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        TypesBean bean = ProxyFactory.createAs(TypesBean.class);

        Map<String, Object> values = new HashMap<String, Object>(8);

        values.put("boolean", "true");
        values.put("byte", "8");
        values.put("short", "16");
        values.put("integer", "32");
        values.put("long", "64");
        values.put("float", "128.0");
        values.put("double", "256.0");
        values.put("character", "a");

        populate(bean, values);

        Assert.assertEquals(true, bean.getBoolean(),
                "Boolean should be set correctly.");
    }

    @Test
    public void testIndexedIsProperty()
    {
        assert BeanManipulator.isPropertyOf(IndexedBean.class, "array[0]") : "array[0] should be a property.";
        assert BeanManipulator.isPropertyOf(IndexedBean.class, "floats[0]") : "floats[0] should be a property.";
        assert BeanManipulator.isPropertyOf(IndexedBean.class,
                "types[0].boolean") : "types[0].boolean should be a property.";

        assert BeanManipulator.isPropertyOf(SourceBean.class,
                "indexed.array[0]") : "indexed.array[0] should be a property.";
        assert BeanManipulator.isPropertyOf(SourceBean.class,
                "indexed.floats[0]") : "indexed.floats[0] should be a property.";
        assert BeanManipulator.isPropertyOf(SourceBean.class,
                "indexed.types[0].boolean") : "indexed.types[0].boolean should be a property.";
    }

    @Test
    public void testIsProperty()
    {
        Assert.assertTrue(BeanManipulator.isPropertyOf(TypesBean.class,
                "boolean"), "Should correctly identify name as valid.");
        Assert.assertFalse(BeanManipulator.isPropertyOf(TypesBean.class,
                "bolean"), "Should correctly identify name as invalid.");

        Assert
                .assertTrue(BeanManipulator.isPropertyOf(NestedBean.class,
                        "nested.boolean"),
                        "Should correctly identify nested as valid.");
        Assert.assertFalse(BeanManipulator.isPropertyOf(NestedBean.class,
                "nested.bolean"),
                "Should correctly identify nested as invalid.");
    }

    @Test
    public void testIndexedTypeOf()
    {
        Assert
                .assertEquals(BeanManipulator.typeOf(IndexedBean.class,
                        "array[0]"), String.class,
                        "array[0] should be of type String.");
        Assert
                .assertEquals(BeanManipulator.typeOf(IndexedBean.class,
                        "floats[0]"), float.class,
                        "floats[0] should be of type float.");
        Assert.assertEquals(BeanManipulator.typeOf(IndexedBean.class,
                "types[0].boolean"), boolean.class,
                "types[0].boolean should be of type boolean.");

        Assert.assertEquals(BeanManipulator.typeOf(IndexedBean.class, "array"),
                String[].class, "array should be of type String[].");

        Assert.assertEquals(BeanManipulator
                .typeOf(SourceBean.class, "array[0]"), String.class,
                "array[0] should be of type String.");

        Assert.assertEquals(BeanManipulator.typeOf(SourceBean.class, "array"),
                String[].class, "array should be of type String[].");

        Assert.assertEquals(BeanManipulator.typeOf(SourceBean.class,
                "indexed.array[0]"), String.class,
                "indexed.array[0] should be of type String.");
        Assert.assertEquals(BeanManipulator.typeOf(SourceBean.class,
                "indexed.floats[0]"), float.class,
                "indexed.floats[0] should be of type float.");
        Assert.assertEquals(BeanManipulator.typeOf(SourceBean.class,
                "indexed.types[0].boolean"), boolean.class,
                "indexed.types[0].boolean should be of type boolean.");

        Assert.assertEquals(BeanManipulator.typeOf(SourceBean.class,
                "indexed.array"), String[].class,
                "indexed.array should be of type String[].");
    }

    @Test
    public void testTypeOf()
    {
        Assert.assertEquals(BeanManipulator.typeOf(TypesBean.class, "boolean"),
                boolean.class, "Should correctly identify type as boolean.");
        Assert.assertNull(BeanManipulator.typeOf(TypesBean.class, "bolean"),
                "Should not be able to find bad property name's type.");

        Assert.assertEquals(BeanManipulator.typeOf(NestedBean.class,
                "nested.boolean"), boolean.class,
                "Should correctly identify nested as boolean.");
        Assert.assertNull(BeanManipulator.typeOf(NestedBean.class,
                "nested.bolean"),
                "Should not be able to find bad nested property name's type.");
    }
}
