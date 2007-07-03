/*
 * Copyright (c) 2001-2006 Brivo Systems, LLC
 * Bethesda, MD 20814
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of Brivo
 * Systems, LLC. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Brivo.
 */
package net.sf.navel.beans;

import java.util.HashMap;
import java.util.Map;

import net.sf.navel.example.RegularBean;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author thomas
 * 
 */
public class SimplePropertyManipulatorTest
{

    @Test
    public void testNullWrapper()
    {
        RegularBean bean = new RegularBean();

        Map<String, Object> values = new HashMap<String, Object>(2);

        values.put("primitive", 1L);
        values.put("wrapper", 1L);
        values.put("object", "foo");

        BeanManipulator.populate(bean, values);

        Assert.assertEquals(1L, bean.getPrimitive(),
                "Should have set primitive correctly.");
        Assert.assertEquals((Long) 1L, bean.getWrapper(),
                "Should have set wrapper correctly.");
        Assert.assertEquals("foo", bean.getObject(),
                "Should have set String correctly.");

        values.put("primitive", null);
        values.put("wrapper", null);
        values.put("object", null);

        BeanManipulator.populate(bean, values);

        Assert.assertEquals(1L, bean.getPrimitive(),
                "Should have left primitive alone.");
        Assert.assertNull(bean.getWrapper(),
                "Should have cleared wrapper correctly.");
        Assert.assertNull(bean.getObject(),
                "Should have cleared String correctly.");
    }
}
