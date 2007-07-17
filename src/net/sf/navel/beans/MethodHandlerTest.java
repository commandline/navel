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

import net.sf.navel.example.AnotherBadDelegatedImpl;
import net.sf.navel.example.BadDelegatedImpl;
import net.sf.navel.example.Delegated;
import net.sf.navel.example.DelegatedBean;
import net.sf.navel.example.DelegatedImpl;
import net.sf.navel.example.PropertyNames;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * This class exercises the delegation mechanism in the DelegateBeanHandler.
 * Delegation support is provided by extending PropertyBeanHandler and accepting
 * an array of delegation targets during contruction. During the dynamic proxy
 * call, anything that cannot be handled by the code in the parent class is then
 * attempted to be match to a delegation target and invoked if possible.
 * 
 * @author cmdln
 */
public class MethodHandlerTest
{

    private static final Logger LOGGER = Logger
            .getLogger(MethodHandlerTest.class);

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
     * Test that construction with a mis-matched DelegationTarget fails.
     */
    @Test
    public void testAttachValidation()
    {
        DelegatedBean bean = ProxyFactory.createAs(DelegatedBean.class);

        try
        {
            ProxyFactory.attach(bean, new BadDelegatedImpl());

            Assert
                    .fail("Attachment should fail if the delegate doesn't implement the delegated interface.");
        }
        catch (Exception e)
        {
            ;
        }

        try
        {
            ProxyFactory.attach(bean, new AnotherBadDelegatedImpl());

            Assert
                    .fail("Attachment should fail if the delegate methods match except for return type.");
        }
        catch (Exception e)
        {
            ;
        }
    }

    /**
     * Test that construction with a mis-matched DelegationTarget fails.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLenientValidation()
    {
        DelegatedBean bean = ProxyFactory.createAs(DelegatedBean.class,
                Delegated.class);

        Delegated delegated = (Delegated) bean;

        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        Assert.assertNotNull(handler, "Lenient construction should work.");

        try
        {
            delegated.doThis(1, 2);

            Assert.fail("Should not have been able to execute correctly!");
        }
        catch (Exception e)
        {
            String message = e.getMessage();

            LogHelper.traceError(LOGGER, e);

            Assert
                    .assertTrue(message.indexOf("InterfaceDelegate") != -1,
                            "Should mention InterfaceDelegate in the exception message.");
        }

        // implements the functional interface and Delegation handler
        DelegatedImpl delegate = new DelegatedImpl();

        ProxyFactory.attach(bean, delegate);

        try
        {
            delegated.doThis(1, 2);
        }
        catch (UnsupportedFeatureException e)
        {
            Assert.fail("Should have been able to execute correctly!");
        }
    }

    /**
     * Test that construction with initial values still works as expected.
     */
    @Test
    public void testInitialValues()
    {
        DelegatedImpl delegate = new DelegatedImpl();
        Map<String, Object> values = new HashMap<String, Object>(3);

        values.put(PropertyNames.RO_PROP, Integer.valueOf(1));
        values.put(PropertyNames.WO_PROP, Integer.valueOf(2));
        values.put(PropertyNames.RW_PROP, Integer.valueOf(3));

        try
        {
            DelegatedBean bean = ProxyFactory.createAs(DelegatedBean.class,
                    values, Delegated.class);
            ProxyFactory.attach(bean, delegate);
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);

            Assert.fail("Should not have gotten an exception.");
        }
    }

    /**
     * Exercise the DelegationTarget mechanism.
     * 
     * @throws UnsupportedFeatureException
     *             Not testing construction, fail.
     * @throws InvalidPropertyValueException
     *             Not testing construction, fail.
     */
    @Test
    public void testDelegation() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        // implements the functional interface and Delegation handler
        DelegatedImpl delegate = new DelegatedImpl();

        DelegatedBean bean = ProxyFactory.createAs(DelegatedBean.class,
                Delegated.class);

        Delegated delegated = (Delegated) bean;

        ProxyFactory.attach(bean, delegate);

        // exercise delegation
        delegated.doThis(Integer.valueOf(1), Integer.valueOf(2));

        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        Map<String, Object> values = handler.propertyValues.copyValues(false);

        Assert.assertNotNull(values.get(PropertyNames.WO_PROP),
                "Write only should be set.");
        Assert.assertEquals(values.get(PropertyNames.WO_PROP), Integer
                .valueOf(1), "Write only should be set correctly.");
        Assert.assertEquals(bean.getReadWrite(), 2,
                "Read write should be set correctly.");

        Integer result = delegated.doThat(Integer.valueOf(2), Integer.valueOf(3));

        // need to fetch values again, since we only every get a shallow copy
        values = handler.propertyValues.copyValues(false);

        Assert.assertNotNull(values.get(PropertyNames.WO_PROP),
                "Write only should be set.");
        Assert.assertEquals(values.get(PropertyNames.WO_PROP), Integer
                .valueOf(2), "Write only should be set correctly.");
        Assert.assertEquals(bean.getReadWrite(), 3,
                "Read write should be set correctly.");
        Assert.assertEquals(result, Integer.valueOf(5),
                "Result should come back correctly.");
    }
}
