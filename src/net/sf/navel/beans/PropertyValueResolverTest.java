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

import net.sf.navel.example.NestedBean;
import net.sf.navel.example.TypesBean;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PropertyValueResolverTest
{

    private static final Logger LOGGER = LogManager
            .getLogger(PropertyValueResolverTest.class);

    @Test
    public void testInit() throws Exception
    {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("long", new Long(63));
        values.put("short", new Short((short) 6));
        values.put("nested.long", new Long(42));
        values.put("nested.short", new Short((short) 4));

        NestedBean bean = ProxyFactory.createAs(NestedBean.class, values);

        LOGGER.debug(bean);

        Assert.assertEquals(63L, bean.getLong(),
                "Regular property should be set.");
        Assert
                .assertNotNull(bean.getNested(),
                        "Parent property should be set.");
        Assert.assertEquals(42L, bean.getNested().getLong(),
                "Nested property should be set.");
    }

    @Test
    public void testNestedTooDeep()
    {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("long", new Long(63));
        values.put("nested.long", new Long(42));
        values.put("nested.too.long", new Long(42));

        try
        {
            ProxyFactory.createAs(NestedBean.class, values);

            Assert
                    .fail("Should not have been able to construct with an overly deep property name.");
        }
        catch (Exception e)
        {
            LOGGER.debug("Caught bad value.");
        }

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
