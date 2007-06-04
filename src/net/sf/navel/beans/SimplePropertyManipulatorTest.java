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

import junit.framework.TestCase;
import net.sf.navel.example.RegularBean;

/**
 * @author thomas
 *
 */
public class SimplePropertyManipulatorTest extends TestCase
{

    public void testNullWrapper()
    {
        RegularBean bean = new RegularBean();
        
        Map<String,Object> values = new HashMap<String,Object>(2);
        
        values.put("primitive", 1L);
        values.put("wrapper", 1L);
        values.put("object", "foo");
        
        BeanManipulator.populate(bean, values);
        
        assertEquals("Should have set primitive correctly.", 1L, bean.getPrimitive());
        assertEquals("Should have set wrapper correctly.", (Long) 1L, bean.getWrapper());
        assertEquals("Should have set String correctly.", "foo", bean.getObject());
        
        values.put("primitive", null);
        values.put("wrapper", null);
        values.put("object", null);
        
        BeanManipulator.populate(bean, values);
        
        assertEquals("Should have left primitive alone.", 1L, bean.getPrimitive());
        assertNull("Should have cleared wrapper correctly.", bean.getWrapper());
        assertNull("Should have cleared String correctly.", bean.getObject());
    }
}
