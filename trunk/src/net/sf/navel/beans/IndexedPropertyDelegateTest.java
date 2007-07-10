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

import net.sf.navel.example.FloatsAsIntegersDelegate;
import net.sf.navel.example.IndexedBean;
import net.sf.navel.example.IndexedIntegersBean;
import net.sf.navel.example.NonIndexedPropertyDelegate;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the additional behavior of indexed property delegation.
 * 
 * @author cmdln
 * 
 */
public class IndexedPropertyDelegateTest
{

    private static final Logger LOGGER = LogManager
            .getLogger(PropertyDelegateTest.class);

    @BeforeMethod
    public void setUp() throws Exception
    {
        Logger root = LogManager.getRootLogger();

        root.removeAllAppenders();

        root.addAppender(new ConsoleAppender(new PatternLayout(
                "%d %-5p [%c] %m%n"), ConsoleAppender.SYSTEM_OUT));
        root.setLevel(Level.DEBUG);
    }

    @Test
    public void testIntegerView()
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class,
                IndexedIntegersBean.class);
        
        indexedBean.setFloats(new float[1]);

        IndexedIntegersBean integersBean = (IndexedIntegersBean) indexedBean;

        integersBean.setIntegers(new int[1]);
        integersBean.setIntegers(0, 42);

        Assert.assertEquals(integersBean.getIntegers(0), 42,
                "Should work with default behavior.");

        ProxyFactory.attach(indexedBean, "integers",
                new FloatsAsIntegersDelegate());

        Assert.assertTrue(ProxyFactory.isAttached(indexedBean, "integers"),
                "Should spot the property delegate.");

        indexedBean.setFloats(0, 37.0f);

        Assert.assertEquals(integersBean.getIntegers(0), 37,
                "Should have gotten the correct view.");

        integersBean.setIntegers(0, 17);

        Assert.assertEquals(indexedBean.getFloats(0), 17.0, 0,
                "Updating the view should have updated the live value.");

        ProxyFactory.detach(indexedBean, "integers");

        JavaBeanHandler handler = ProxyFactory.getHandler(indexedBean);

        Assert.assertFalse(ProxyFactory.isAttached(indexedBean, "integers"),
                "Should not longer have the property delegate.");

        Assert.assertEquals(integersBean.getIntegers(0), 42,
                "Detaching should restore original behavior.");

        Assert.assertEquals(indexedBean.getFloats(0), 17.0, 0,
                "Live value should be permanently changed.");

        Assert.assertEquals(handler.propertyValues.copyValues().size(), 2,
                "Should have two entries.");
    }

    @Test
    public void breakValidation()
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        try
        {
            ProxyFactory.attach(indexedBean, "array",
                    new NonIndexedPropertyDelegate());

            Assert.fail("Should not be able to attach a mismatched delegate.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }
    }

}
