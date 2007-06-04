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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.sf.navel.example.BadBeanImpl;
import net.sf.navel.example.IndexedBean;
import net.sf.navel.example.NestedBean;
import net.sf.navel.example.ReadWriteBean;
import net.sf.navel.example.SourceBean;
import net.sf.navel.example.TargetBean;
import net.sf.navel.example.TypesBean;
import net.sf.navel.test.PropertyNames;

import org.apache.log4j.Logger;

/**
 * Exercises the two primary activities of the bean manipulator.
 * 
 * @author cmdln
 */
public class BeanManipulatorTest extends TestCase
{
    private static final Logger LOGGER = Logger
            .getLogger(BeanManipulatorTest.class);

    public BeanManipulatorTest(String name)
    {
        super(name);
    }

    /**
     * Exercise bean description on the ReadWriteBean backed by the
     * ProperyBeanHandler.
     * 
     * @throws InvalidPropertyValueException
     *             Not testing construction, error on problem.
     * @throws UnsupportedFeatureException
     *             Not testing construction, error on problem.
     */
    public void testDescribeReadWrite() throws InvalidPropertyValueException,
            UnsupportedFeatureException
    {
        Map<String, Object> values = new HashMap<String, Object>(3);

        values.put(PropertyNames.RO_PROP, 1);
        values.put(PropertyNames.WO_PROP, 2);
        values.put(PropertyNames.RW_PROP, 3);

        PropertyBeanHandler<ReadWriteBean> handler = new PropertyBeanHandler<ReadWriteBean>(
                ReadWriteBean.class, values, false);

        ReadWriteBean bean = handler.getProxy();

        values.remove(PropertyNames.WO_PROP);

        Map<String, Object> descriptions = describe(bean);

        assertEquals("Description should match initial values.", values,
                descriptions);
    }

    /**
     * Test that BeanManipulator doesn't return a null Map from describe when
     * there is nothing to describe.
     */
    public void testNoProperties()
    {
        BadBeanImpl bean = new BadBeanImpl();

        Map<String, Object> values = describe(bean);

        assertNotNull("Describe should never return null.", values);
        assertTrue("Describe should return empty map when there's a problem.",
                values.isEmpty());
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
    public void testPopulateReadWrite() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        Map<String, Object> values = new HashMap<String, Object>(3);
        values.put(PropertyNames.RO_PROP, 1);
        values.put(PropertyNames.WO_PROP, 2);
        values.put(PropertyNames.RW_PROP, 3);

        PropertyBeanHandler<ReadWriteBean> handler = new PropertyBeanHandler<ReadWriteBean>(
                ReadWriteBean.class);
        ReadWriteBean bean = handler.getProxy();

        populate(bean, values);

        assertEquals("Bean read only should not be set.", 0, bean.getReadOnly());
        values = handler.getValues();
        LOGGER.debug(values);
        assertNotNull("Bean write only should be set.", values
                .get(PropertyNames.WO_PROP));
        assertEquals("Bean write only should be set correctly.",
                new Integer(2), values.get(PropertyNames.WO_PROP));
        assertEquals("bean read/write should be set.", 3, bean.getReadWrite());
    }

    /**
     * Test extracting from and inject into concrete beans.
     */
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

        assertNotNull("Extracted values should be set.", values);
        assertEquals("Check source foo.", new Byte(fooSource), values
                .get("foo"));
        assertEquals("Check source bar.", new Short(barSource), values
                .get("bar"));
        assertEquals("Check source baz.", new Integer(bazSource), values
                .get("baz"));
        assertEquals("Check source quux.", new Float(quuxSource), values
                .get("quux"));

        TargetBean targetBean = new TargetBean();

        targetBean.setFoo(fooTarget);
        targetBean.setBar(barTarget);
        targetBean.setBaz(bazTarget);
        targetBean.setQuux(quuxTarget);

        populate(targetBean, values);

        assertEquals("Check target foo.", fooTarget, targetBean.getFoo());
        assertEquals("Check target bar.", barSource, targetBean.getBar());
        assertEquals("Check target baz.", bazSource, targetBean.getBaz());
        assertEquals("Check target quux.", quuxTarget, targetBean.getQuux(),
                0.0d);
    }

    /**
     * Test population of nested properties, using "dot notation".
     * 
     * @throws InvalidPropertyValueException
     *             Ignore construction errors.
     * @throws UnsupportedFeatureException
     *             Ignore construction errors.
     */
    public void testPopulateNested() throws InvalidPropertyValueException,
            UnsupportedFeatureException
    {
        PropertyBeanHandler<TypesBean> nestedHandler = new PropertyBeanHandler<TypesBean>(
                TypesBean.class);
        TypesBean nestedBean = nestedHandler.getProxy();

        Map<String, Object> values = new HashMap<String, Object>(2);

        values.put("nested", nestedBean);

        PropertyBeanHandler<NestedBean> handler = new PropertyBeanHandler<NestedBean>(
                NestedBean.class, values, false);
        NestedBean bean = handler.getProxy();

        assertNotNull("Nested bean should not be null.", bean.getNested());

        values.clear();

        // test convert values from a String to a Boolean
        values.put("nested.boolean", "true");
        values.put("nested.long", new Long(128));

        populate(bean, values);

        assertEquals("Nested boolean should be set correctly.", true, bean
                .getNested().getBoolean());
        assertEquals("Nested long should be set correctly.", 128L, bean
                .getNested().getLong());

        values = handler.getValues();
        LOGGER.debug(values);
        LOGGER.debug(nestedHandler.getValues());
    }

    /**
     * Test population of indexed properties.
     * 
     * @throws InvalidPropertyValueException
     *             Ignore construction errors.
     * @throws UnsupportedFeatureException
     *             Ignore construction errors.
     */
    public void testPopulateIndexed() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        PropertyBeanHandler<IndexedBean> indexedHandler = new PropertyBeanHandler<IndexedBean>(
                IndexedBean.class);
        IndexedBean indexedBean = indexedHandler.getProxy();

        PropertyBeanHandler<TypesBean> typeHandler = new PropertyBeanHandler<TypesBean>(
                TypesBean.class);
        TypesBean typesBean = typeHandler.getProxy();

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

        assertEquals("First String element should be set correctly.", "foo",
                indexedBean.getArray(0));
        assertEquals("Second String element should be set correctly.", "bar",
                indexedBean.getArray(1));

        assertEquals("First float element should be set correctly.", 32.0,
                indexedBean.getFloats(0), 0);
        assertEquals("Second float element should be set correctly.", 64.0,
                indexedBean.getFloats(1), 0);

        assertEquals(
                "Boolean property of first types element should be set correctly.",
                true, indexedBean.getTypes(0).getBoolean());
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
    public void testBadIndexed() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        PropertyBeanHandler<IndexedBean> indexedHandler = new PropertyBeanHandler<IndexedBean>(
                IndexedBean.class);
        IndexedBean indexedBean = indexedHandler.getProxy();

        indexedBean.setArray(new String[2]);
        indexedBean.setFloats(new float[2]);
        indexedBean.setTypes(new TypesBean[1]);

        Map<String, Object> values = new HashMap<String, Object>(5);

        values.put("array[false]", "foo");
        values.put("array[1.0d]", "bar");
        values.put("floats[foo]", new Float(32.0));
        values.put("floats[1.0d]", new Float(64.0));
        values.put("types[bar].boolean", Boolean.TRUE);

        populate(indexedBean, values);

        assertNull("First String element should not be set.", indexedBean
                .getArray(0));
        assertNull("Second String element should not be set.", indexedBean
                .getArray(1));

        assertEquals("First float element should not be set.", 0.0, indexedBean
                .getFloats(0), 0);
        assertEquals("Second float element should not be set.", 0.0,
                indexedBean.getFloats(1), 0);

        assertNull("First types element should not be set", indexedBean
                .getTypes(0));
    }

    /**
     * Test that String to primitive conversion works as expected.
     * 
     * @throws InvalidPropertyValueException
     *             Ignore construction errors.
     * @throws UnsupportedFeatureException
     *             Ignore construction errors.
     */
    public void testStringConversion() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        PropertyBeanHandler<TypesBean> handler = new PropertyBeanHandler<TypesBean>(
                TypesBean.class);
        TypesBean bean = handler.getProxy();

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

        assertEquals("Boolean should be set correctly.", true, bean
                .getBoolean());
    }

    public void testIsProperty()
    {
        assertTrue("Should correctly identify name as valid.", BeanManipulator
                .isPropertyOf(TypesBean.class, "boolean"));
        assertFalse("Should correctly identify name as invalid.", BeanManipulator
                .isPropertyOf(TypesBean.class, "bolean"));
        assertTrue("Should correctly identify nested as valid.", BeanManipulator
                .isPropertyOf(NestedBean.class, "nested.boolean"));
        assertFalse("Should correctly identify nested as invalid.", BeanManipulator
                .isPropertyOf(NestedBean.class, "nested.bolean"));
    }
}
