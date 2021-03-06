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
import java.util.HashMap;
import java.util.Map;

import net.sf.navel.example.AnotherConflictBean;
import net.sf.navel.example.ConflictBean;
import net.sf.navel.example.Delegated;
import net.sf.navel.example.DelegatedImpl;
import net.sf.navel.example.IdentityBean;
import net.sf.navel.example.NestedBean;
import net.sf.navel.example.NestedConstructor;
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
 * Test case for exercises the construction and general constraints.
 * 
 * @author cmdln
 */
public class ProxyFactoryTest
{

    private static final Logger LOGGER = LogManager
            .getLogger(ProxyFactoryTest.class);

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
        final TypesBean test = ProxyFactory.createAs(TypesBean.class,
                Delegated.class);

        ProxyFactory.attach(test, new DelegatedImpl());

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

        ObjectOutputStream out = null;

        try
        {
            out = new ObjectOutputStream(byteOutput);

            out.writeObject(test);
        }
        catch (IOException e)
        {
            LogHelper.traceError(LOGGER, e);

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

        TypesBean result = null;

        ObjectInputStream input = null;

        try
        {
            input = new ObjectInputStream(new ByteArrayInputStream(byteOutput
                    .toByteArray()));

            result = (TypesBean) input.readObject();

            Assert
                    .assertFalse(test == result,
                            "Should not be the same object.");
            Assert.assertEquals(test, result, "Should be equivalent object.");
        }
        catch (IOException e)
        {
            LogHelper.traceError(LOGGER, e);

            Assert.fail("Should be able to open bytes into a stream.");
        }
        catch (ClassNotFoundException e)
        {
            LogHelper.traceError(LOGGER, e);

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

        try
        {
            ProxyManipulator.put(result, "integer", 1);
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);

            Assert
                    .fail("Should have been able to use features that rely on introspection.");
        }

        try
        {
            ProxyManipulator.put(result, "foo", 1);

            Assert
                    .fail("Should have been able to use features that rely on introspection.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }
    }

    /**
     * Exercise the data type validation logic enforced during construction.
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

    @Test
    public void testCopy()
    {
        TypesBean source = ProxyFactory.createAs(TypesBean.class,
                Delegated.class);

        Assert.assertTrue(source instanceof Delegated);

        source.setBoolean(true);

        TypesBean copy = ProxyFactory.copyAs(TypesBean.class, source);

        Assert.assertTrue(copy instanceof Delegated);

        Assert.assertEquals(source, copy, "Should initially be the same.");
        Assert.assertNotSame(source, copy, "Should not be identical, though.");

        copy.setBoolean(false);

        Assert.assertFalse(source.equals(copy),
                "Should be able to change one and not affect the other.");
    }

    @Test
    public void testView()
    {
        TypesBean source = ProxyFactory.createAs(TypesBean.class,IdentityBean.class);

        source.setBoolean(true);

        Assert.assertTrue(source instanceof IdentityBean);
        
        IdentityBean idBean = (IdentityBean) source;
        
        idBean.setID(1000);

        IdentityBean copy = ProxyFactory.viewAs(IdentityBean.class, source);

        Assert.assertTrue(copy instanceof IdentityBean);
        Assert.assertFalse(copy instanceof TypesBean);

        Assert.assertFalse(source.equals(copy), "Should not initially be the same.");
        Assert.assertNotSame(source, copy, "Should not be identical, either.");
        
        copy.setID(2000);
        
        Assert.assertEquals(idBean.getID(), 1000);
        Assert.assertEquals(copy.getID(), 2000);
        Assert.assertFalse(ProxyFactory.getHandler(copy).propertyValues.isPropertyOf("boolean"));
    }

    @Test
    public void testNestedShallow()
    {
        NestedBean source = ProxyFactory.createAs(NestedBean.class);
        source.setNested(ProxyFactory.createAs(TypesBean.class));
        source.setBoolean(true);
        source.getNested().setBoolean(true);

        NestedBean copy = ProxyFactory.copyAs(NestedBean.class, source);

        Assert.assertEquals(source, copy, "Should initially be the same.");
        Assert.assertNotSame(source, copy, "Should not be identical, though.");

        Assert.assertEquals(source.getNested(), copy.getNested(),
                "Nested should initially be the same.");
        Assert.assertSame(source.getNested(), copy.getNested(),
                "Nested should be identical.");

        copy.setBoolean(false);
        copy.getNested().setBoolean(false);

        Assert.assertFalse(source.equals(copy),
                "Should be able to change one and not affect the other.");

        Assert.assertEquals(copy.getNested(), source.getNested(),
                "On a shallow copy, changes to nested should affect both.");
    }

    @Test
    public void testNestedDeep()
    {
        NestedBean source = ProxyFactory.createAs(NestedBean.class);
        source.setNested(ProxyFactory.createAs(TypesBean.class));
        source.setBoolean(true);
        source.getNested().setBoolean(true);

        NestedBean copy = ProxyFactory.copyAs(NestedBean.class, source, true);

        Assert.assertEquals(source, copy, "Should initially be the same.");
        Assert.assertNotSame(source, copy, "Should not be identical, though.");

        Assert.assertEquals(source.getNested(), copy.getNested(),
                "Nested should initially be the same.");
        Assert.assertNotSame(source.getNested(), copy.getNested(),
                "Nested should not be identical.");

        copy.setBoolean(false);
        copy.getNested().setBoolean(false);

        Assert.assertFalse(source.equals(copy),
                "Should be able to change one and not affect the other.");

        Assert.assertFalse(copy.getNested().equals(source.getNested()),
                "On a deep copy, changes to nested should not affect both.");
    }

    @Test
    public void testUnmodifiable()
    {
        TypesBean source = ProxyFactory.createAs(TypesBean.class);
        source.setBoolean(true);

        TypesBean unmodifiable = ProxyFactory.unmodifiableObjectAs(
                TypesBean.class, source);

        Assert.assertTrue(ProxyManipulator.isSet(unmodifiable, "boolean"));
        Assert.assertEquals(unmodifiable.getBoolean(), true);

        try
        {
            unmodifiable.setBoolean(false);

            Assert.fail("Should not be able to write through interface.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.put(unmodifiable, "boolean", false);

            Assert.fail("Should not be able to write dynamically.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            Map<String, Object> values = new HashMap<String, Object>(1);
            values.put("boolean", false);

            ProxyManipulator.putAll(unmodifiable, values);

            Assert.fail("Should not be able to write all dynamically.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.clear(unmodifiable, "boolean");

            Assert.fail("Should not be able to clear dynamically.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.clear(unmodifiable);

            Assert.fail("Should not be able to clear all dynamically.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }
    }

    @Test
    public void testNestedUnmodifiable()
    {
        NestedBean source = ProxyFactory.createAs(NestedBean.class);
        source.setNested(ProxyFactory.createAs(TypesBean.class));
        source.getNested().setBoolean(true);

        NestedBean unmodifiable = ProxyFactory.unmodifiableObjectAs(
                NestedBean.class, source);

        Assert.assertTrue(ProxyManipulator.isSet(unmodifiable.getNested(),
                "boolean"));
        Assert.assertEquals(unmodifiable.getNested().getBoolean(), true);

        try
        {
            unmodifiable.setBoolean(true);

            Assert.fail("Should not be able to write through interface.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            unmodifiable.getNested().setBoolean(false);

            Assert.fail("Should not be able to write through interface.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.put(unmodifiable.getNested(), "boolean", false);

            Assert.fail("Should not be able to write dynamically.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            Map<String, Object> values = new HashMap<String, Object>(1);
            values.put("boolean", false);

            ProxyManipulator.putAll(unmodifiable.getNested(), values);

            Assert.fail("Should not be able to write all dynamically.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.clear(unmodifiable.getNested(), "boolean");

            Assert.fail("Should not be able to clear dynamically.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyManipulator.clear(unmodifiable.getNested());

            Assert.fail("Should not be able to clear all dynamically.");
        }
        catch (UnsupportedOperationException e)
        {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith(
                    "This bean is immutable."));

            LogHelper.traceError(LOGGER, e);
        }
    }

    @Test
    public void testConflict()
    {
        try
        {
            ProxyFactory.createAs(TypesBean.class, ConflictBean.class);

            Assert
                    .fail("The reflection code should prevent this particular proxy from being created.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }

        try
        {
            ProxyFactory.createAs(TypesBean.class, AnotherConflictBean.class);

            Assert
                    .fail("The validation code should prevent this particular proxy from being created.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }
    }
    
    @Test
    public void testImmutableWithConstructorDelegate()
    {
        ProxyFactory.register(NestedBean.class, new NestedConstructor());
        
        NestedBean nestedBean = ProxyFactory.createAs(NestedBean.class);
        
        Assert.assertNotNull(nestedBean.getNested(), "Constructor should have initialized nested property.");
        
        NestedBean immutableCopy = ProxyFactory.unmodifiableObjectAs(NestedBean.class, nestedBean);
        
        Assert.assertNotNull(immutableCopy.getNested(), "Copy should have carried over nested property.");
    }
}
