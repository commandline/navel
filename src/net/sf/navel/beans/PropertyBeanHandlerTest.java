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
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sf.navel.example.AltBoolean;
import net.sf.navel.example.BadPropertyBean;
import net.sf.navel.example.IdentityBean;
import net.sf.navel.example.IndexedBean;
import net.sf.navel.example.ReadWriteBean;
import net.sf.navel.example.SensitiveBean;
import net.sf.navel.example.TypesBean;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test case for exercises the PropertyBeanHandler, including failure modes.
 * 
 * @author cmdln
 * @version $Revision: 1.4 $, $Date: 2005/09/16 15:27:44 $
 */
public class PropertyBeanHandlerTest
{

    /**
     * Make sure that an interface written to be used with the
     * PropertyBeanHandler can be introspected like any other JavaBean.
     * 
     * @throws Exception
     *             Force an error if introspection fails.
     */
    @Test
    public void testIntrospection() throws IntrospectionException
    {
        BeanInfo beanInfo = Introspector.getBeanInfo(TypesBean.class);

        Assert.assertNotNull(beanInfo, "Bean info should not be null.");

        PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();

        Assert.assertNotNull(properties,
                "Property descriptors should not be null.");
        Assert.assertTrue(0 != properties.length,
                "Property descriptors should not be empty.");
    }

    /**
     * Make sure that a Navel Bean serializes correctly, without error.
     * 
     * @throws UnsupportedFeatureException
     *             Construction should work fine, error otherwise.
     * @throws InvalidPropertyValueException
     *             Construction should work fine, error otherwise.
     */
    @Test
    public void testSerialization() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        final TypesBean test = ProxyFactory.createAs(TypesBean.class);

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

        ObjectOutputStream out = null;

        try
        {
            out = new ObjectOutputStream(byteOutput);

            out.writeObject(test);
        }
        catch (IOException e)
        {
            e.printStackTrace();

            Assert.fail("Should be able to serialize just to bytes.");
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException e)
                {
                    // do nothing
                }
            }
        }

        ObjectInputStream input = null;

        try
        {
            input = new ObjectInputStream(new ByteArrayInputStream(byteOutput
                    .toByteArray()));

            TypesBean result = (TypesBean) input.readObject();

            Assert
                    .assertFalse(test == result,
                            "Should not be the same object.");
            Assert.assertEquals(test, result, "Should be equivalent object.");
        }
        catch (IOException e)
        {
            e.printStackTrace();

            Assert.fail("Should be able to open bytes into a stream.");
        }
        catch (ClassNotFoundException e)
        {
            Assert.fail("Should not have any class loading issues.");
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                    // do nothing
                }
            }
        }
    }

    /**
     * Test that the invoke method of the PropertyBeanHandler successfully
     * discriminiates each property access, for a mixture of readable and
     * writable properties.
     * 
     * @throws UnsupportedFeatureException
     *             Construction should work fine, error otherwise.
     * @throws InvalidPropertyValueException
     *             Construction should work fine, error otherwise.
     */
    @Test
    public void testReadWrite() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        int readOnly = 1;
        int writeOnly = 2;
        int readWrite = 3;

        // can only set read-only via the underlying map
        Map<String, Object> values = new HashMap<String, Object>(1);
        values.put("readOnly", new Integer(readOnly));

        ReadWriteBean bean = ProxyFactory.createAs(ReadWriteBean.class);

        // write-only
        bean.setWriteOnly(writeOnly);

        // read/write
        bean.setReadWrite(readWrite);

        // can only check write-only via the underlying map
        // TODO restore when new utility class is complete
//        values = BeanManipulator.getNavelHandler(bean).propertyHandler
//                .getValues();
//
//        Assert.assertEquals(readOnly, bean.getReadOnly(),
//                "readOnly should equal 1");
//        Assert.assertEquals(new Integer(writeOnly), values.get("writeOnly"),
//                "writeOnly should equal 2");
//        Assert.assertEquals(readWrite, bean.getReadWrite(),
//                "readWrite should equal 3");
    }

    /**
     * Exercise the data type validation logic embedded in the
     * PropertyBeanHandler.
     * 
     * @throws UnsupportedFeatureException
     *             Construction should work fine, error otherwise.
     */
    @Test
    public void testBadData() throws UnsupportedFeatureException
    {
        String[] badData = new String[]
        { "boolean", "byte", "short", "integer", "long", "float", "double",
                "character", };

        for (int i = 0; i < badData.length; i++)
        {
            Map<String, Object> values = new HashMap<String, Object>(1);
            values.put(badData[i], badData[i]);

            try
            {
                ProxyFactory.createAs(TypesBean.class, values);
                Assert.fail("Should catch bad data on construction.");
            }
            catch (InvalidPropertyValueException e)
            {
                ;
            }

            values.clear();
            values.put("foo", "foo");

            try
            {
                ProxyFactory.createAs(TypesBean.class, values);
                Assert.fail("Should catch bad data on construction.");
            }
            catch (InvalidPropertyValueException e)
            {
                ;
            }
        }
    }

    //
    // @Test
    // public void testBadPut() throws UnsupportedFeatureException,
    // InvalidPropertyValueException
    // {
    // PropertyHandler handler = new PropertyHandler(new Class[] {
    // TypesBean.class});
    //
    // try
    // {
    // handler.put("foo", "bar");
    // Assert.fail("Should have errored on bad values.");
    // }
    // catch (Exception e)
    // {
    // ;
    // }
    // }

    // @Test
    // public void testBadPutAll() throws UnsupportedFeatureException,
    // InvalidPropertyValueException
    // {
    // PropertyHandler<TypesBean> handler = new PropertyHandler<TypesBean>(
    // TypesBean.class);
    //
    // Map<String, Object> badValues = new HashMap<String, Object>(1);
    // badValues.put("foo", "bar");
    //
    // try
    // {
    // handler.putAll(badValues);
    // Assert.fail("Should have errored on bad values.");
    // }
    // catch (Exception e)
    // {
    // ;
    // }
    // }

    /**
     * Test that the PropertyBeanHandler complains on constructrion when passed
     * an interface that doesn't actually define any properties.
     */
    @Test
    public void testNoProperties()
    {
        try
        {
            ProxyFactory.createAs(BadPropertyBean.class);
            Assert.fail("Should have thrown an exception on construction.");
        }
        catch (UnsupportedFeatureException e)
        {
            ;
        }
        catch (InvalidPropertyValueException e)
        {
            ;
        }
    }

    /**
     * Exercise the indexed property support of PropertyBeanHandler. Need to be
     * sure that the regular assignment of Object[] type properties works, as
     * well as the indexed accesses, including all the requisite type munging,
     * except for primitives
     * 
     * @throws InvalidPropertyValueException
     *             Construction should work fine, error otherwise.
     * @throws UnsupportedFeatureException
     *             Construction should work fine, error otherwise.
     */
    @Test
    public void testObjectArray() throws InvalidPropertyValueException,
            UnsupportedFeatureException
    {
        Map<String, Object> values = new HashMap<String, Object>(1);
        values.put("array", new String[3]);

        IndexedBean bean = ProxyFactory.createAs(IndexedBean.class, values);

        String[] data = new String[]
        { "foo", "bar", "baz" };

        for (int i = 0; i < data.length; i++)
        {
            bean.setArray(i, data[i]);
            Assert.assertEquals(data[i], bean.getArray(i), "Element " + i
                    + " should equal " + data[i]);
        }

        String[] beanData = bean.getArray();
        Assert.assertNotNull(beanData, "Array from bean should not be null.");
        Assert.assertTrue(Arrays.equals(data, beanData),
                "Array from bean should equal original data.");

        for (int i = 0; i < beanData.length; i++)
        {
            Assert.assertEquals(data[i], beanData[i],
                    "Original data and bean should match for element " + i
                            + ".");
        }
    }

    /**
     * Same test as testObjectArray except performed on a primitive array,
     * exercises the PrimitiveSupport class in particular.
     * 
     * @throws InvalidPropertyValueException
     *             Construction should work fine, error otherwise.
     * @throws UnsupportedFeatureException
     *             Construction should work fine, error otherwise.
     */
    @Test
    public void testPrimitiveArray() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        Map<String, Object> values = new HashMap<String, Object>(1);
        values.put("floats", new float[3]);

        IndexedBean bean = ProxyFactory.createAs(IndexedBean.class, values);

        float[] data = new float[]
        { 1.0f, 2.0f, 3.0f };

        for (int i = 0; i < data.length; i++)
        {
            bean.setFloats(i, data[i]);
            Assert.assertEquals(0.0f, data[i], bean.getFloats(i), i
                    + " should equal " + data[i] + "Element ");
        }

        float[] beanData = bean.getFloats();
        Assert.assertNotNull(beanData, "Array from bean should not be null.");

        Assert.assertTrue(Arrays.equals(data, beanData),
                "Array from bean should equal original data.");

        for (int i = 0; i < beanData.length; i++)
        {
            Assert.assertEquals(0.0f, data[i], beanData[i],
                    "Original data and bean should match for element " + i
                            + ".");
        }
    }

    /**
     * Test support for the "is&lt;property&gt;" convention for reading boolean
     * properties.
     * 
     * @throws InvalidPropertyValueException
     *             Construction should work fine, error otherwise.
     * @throws UnsupportedFeatureException
     *             Construction should work fine, error otherwise.
     */
    @Test
    public void testBooleanAlt() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        Map<String, Object> values = new HashMap<String, Object>(3);
        values.put("primitiveAlt", Boolean.TRUE);

        AltBoolean bean = ProxyFactory.createAs(AltBoolean.class, values);

        Assert.assertTrue(bean.isPrimitiveAlt(),
                "Boolean alt using is should work.");
    }

    @Test
    public void testToString()
    {
        Map<String, Object> values = new HashMap<String, Object>(3);
        values.put("username", "foo");
        values.put("password", "bar");

        SensitiveBean bean = ProxyFactory.createAs(SensitiveBean.class, values);

        Assert.assertTrue(bean.toString().indexOf("username") != -1,
                "Username should be present.");
        Assert.assertTrue(bean.toString().indexOf("password") == -1,
                "Password should not be present.");
    }

    @Test
    public void testEquals()
    {
        ReadWriteBean bean = ProxyFactory.createAs(ReadWriteBean.class);

        Assert.assertFalse(bean.equals(null), "Nothing should equal null.");
    }

    @Test
    public void testID()
    {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("ID", 100);

        IdentityBean bean = ProxyFactory.createAs(IdentityBean.class, values);

        Assert.assertEquals(100, bean.getID(), "ID should be usable.");
    }
}
