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

import java.util.HashMap;
import java.util.Map;

import net.sf.navel.example.AncestorBean;
import net.sf.navel.example.NestedBean;
import net.sf.navel.example.TypesBean;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class InitialValuesResolverTest
{

    private static final Logger LOGGER = LogManager
            .getLogger(InitialValuesResolverTest.class);

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

    @Test
    public void testInit() throws Exception
    {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("long", Long.valueOf(63));
        values.put("short", Short.valueOf((short) 6));
        values.put("nested.long", Long.valueOf(42));
        values.put("nested.short", Short.valueOf((short) 4));

        NestedBean bean = ProxyFactory.createAs(NestedBean.class, values);

        LOGGER.debug(bean);

        Assert.assertEquals(bean.getLong(), 63L,
                "Regular property should be set.");
        Assert
                .assertNotNull(bean.getNested(),
                        "Parent property should be set.");
        Assert.assertEquals(bean.getNested().getLong(), 42L,
                "Nested property should be set.");
    }

    @Test
    public void testNestedDeeply()
    {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("name", "foo");
        values.put("child.long", Long.valueOf(63));
        values.put("child.nested.long", Long.valueOf(42));

        AncestorBean bean = ProxyFactory.createAs(AncestorBean.class, values);

        Assert.assertEquals(bean.getChild().getNested().getLong(), 42L,
                "Should have been able to resolve arbitrarily deep.");
    }

    @Test
    public void testFlatten() throws Exception
    {
        NestedBean bean = ProxyFactory.createAs(NestedBean.class);

        bean.setLong(63L);
        bean.setShort((short) 6);
        bean.setNested(ProxyFactory.createAs(TypesBean.class));
        bean.getNested().setLong(42L);
        bean.getNested().setShort((short) 4);

        Map<String, Object> values = describe(bean, true);

        LOGGER.debug(values);

        Assert.assertEquals(10, values.size(),
                "Should have correct number of properties.");

        values = describe(bean);

        LOGGER.debug(values);

        Assert.assertEquals(9, values.size(),
                "Should have correct number of properties.");
    }
}
