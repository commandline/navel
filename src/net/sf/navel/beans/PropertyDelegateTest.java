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

import net.sf.navel.example.StringBean;
import net.sf.navel.example.CharacterAsStringDelegate;
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
 * Test that property delegate works as desired.
 * 
 * @author cmdln
 * 
 */
public class PropertyDelegateTest
{
    
    private static final Logger LOGGER = LogManager.getLogger(PropertyDelegateTest.class);

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
    public void testStringView()
    {
        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class,
                StringBean.class);

        StringBean stringBean = (StringBean) typesBean;

        stringBean.setString("foo");

        Assert.assertEquals(stringBean.getString(), "foo",
                "Should work with default behavior.");

        ProxyFactory.attach(typesBean, "string",
                new CharacterAsStringDelegate());

        Assert.assertTrue(
                ProxyFactory.isAttached(typesBean, "string"),
                "Should spot the property delegate.");

        typesBean.setCharacter('a');

        Assert.assertEquals(stringBean.getString(), "a",
                "Should have gotten the correct view.");

        stringBean.setString("b");

        Assert.assertEquals(typesBean.getCharacter(), 'b',
                "Updating the view should have updated the live value.");

        ProxyFactory.detach(typesBean, "string");

        JavaBeanHandler handler = ProxyFactory.getHandler(typesBean);

        Assert.assertFalse(ProxyFactory
                .isAttached(typesBean, "string"),
                "Should not longer have the property delegate.");

        Assert.assertEquals(stringBean.getString(), "foo",
                "Detaching should restore original behavior.");

        Assert.assertEquals(typesBean.getCharacter(), 'b',
                "Live value should be permanently changed.");

        Assert.assertEquals(handler.propertyValues.copyValues().size(), 2,
                "Should have two entries.");
    }

    @Test
    public void breakValidation()
    {
        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class,
                StringBean.class);

        try
        {
            ProxyFactory.attach(typesBean, "boolean",
                    new CharacterAsStringDelegate());

            Assert.fail("Should not be able to attach a mismatched delegate.");
        }
        catch (Exception e)
        {
            LogHelper.traceError(LOGGER, e);
        }
    }
}
