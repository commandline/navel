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

import net.sf.navel.example.BadBeanImpl;
import net.sf.navel.example.ConcreteDelegatedImpl;
import net.sf.navel.example.DelegatedBean;
import net.sf.navel.example.DelegatedImpl;
import net.sf.navel.log.LogHelper;
import net.sf.navel.test.PropertyNames;

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
 * @version $Revision: 1.4 $, $Date: 2005/09/16 15:27:23 $
 */
public class DelegateBeanHandlerTest
{
    private static final Logger LOGGER = Logger
            .getLogger(DelegateBeanHandlerTest.class);

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
    public void testConstructionValidation()
    {
        try
        {
            BadBeanImpl badDelegate = new BadBeanImpl();

            new DelegateBeanHandler<DelegatedBean>(DelegatedBean.class,
                    new DelegationTarget[]
                    { badDelegate });
            Assert
                    .fail("Construction should fail if the delegate doesn't implement the delegated interface.");
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
     * Test that construction with a mis-matched DelegationTarget fails.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLenientValidation()
    {
        DelegateBeanHandler<DelegatedBean> handler = new DelegateBeanHandler<DelegatedBean>(
                DelegatedBean.class, DelegateBeanHandler.DEFAULT_RESOLVE, false);

        Assert.assertNotNull(handler.getProxy(),
                "Lenient construction should work.");

        DelegatedBean bean = handler.getProxy();
        try
        {
            bean.doThis(1, 2);

            Assert.fail("Should not have been able to execute correctly!");
        }
        catch (UnsupportedFeatureException e)
        {
            String message = e.getMessage();

            Assert
                    .assertTrue(message.indexOf("DelegationTarget") != -1,
                            "Should mention DelegationTarget in the exception message.");
        }

        // implements the functional interface and Delegation handler
        DelegatedImpl delegate = new DelegatedImpl();

        handler.attachDelegationTarget(delegate);

        try
        {
            bean.doThis(1, 2);
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

        values.put(PropertyNames.RO_PROP, new Integer(1));
        values.put(PropertyNames.WO_PROP, new Integer(2));
        values.put(PropertyNames.RW_PROP, new Integer(3));

        try
        {
            new DelegateBeanHandler<DelegatedBean>(DelegatedBean.class, values,
                    delegate);
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

        // supports the combined property and function interface via the
        // delegation handler
        DelegateBeanHandler<DelegatedBean> handler = new DelegateBeanHandler<DelegatedBean>(
                DelegatedBean.class, delegate);

        // our bean with properties plus delegated methods
        DelegatedBean bean = handler.getProxy();

        // exercise delegation
        bean.doThis(new Integer(1), new Integer(2));

        Map<String, Object> values = handler.getValues();

        Assert.assertNotNull(values.get(PropertyNames.WO_PROP),
                "Write only should be set.");
        Assert.assertEquals(new Integer(1), values.get(PropertyNames.WO_PROP),
                "Write only should be set correctly.");
        Assert.assertEquals(2, bean.getReadWrite(),
                "Read write should be set correctly.");

        Integer result = bean.doThat(new Integer(2), new Integer(3));

        // need to fetch values again, since we only every get a shallow copy
        values = handler.getValues();

        Assert.assertNotNull(values.get(PropertyNames.WO_PROP),
                "Write only should be set.");
        Assert.assertEquals(new Integer(2), values.get(PropertyNames.WO_PROP),
                "Write only should be set correctly.");
        Assert.assertEquals(3, bean.getReadWrite(),
                "Read write should be set correctly.");
        Assert.assertEquals(new Integer(5), result,
                "Result should come back correctly.");
    }

    @Test
    public void testWithInheritance()
    {
        ConcreteDelegatedImpl delegate = new ConcreteDelegatedImpl();
        Map<String, Object> values = new HashMap<String, Object>(3);

        values.put(PropertyNames.RO_PROP, new Integer(1));
        values.put(PropertyNames.WO_PROP, new Integer(2));
        values.put(PropertyNames.RW_PROP, new Integer(3));

        try
        {
            new DelegateBeanHandler<DelegatedBean>(DelegatedBean.class, values,
                    new DelegationTarget[]
                    { delegate });
        }
        catch (Exception e)
        {
            LOGGER.error(e);
            Assert.fail("Should not have gotten an exception.");
        }
    }
}
