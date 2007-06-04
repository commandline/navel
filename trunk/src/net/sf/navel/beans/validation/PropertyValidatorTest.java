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
package net.sf.navel.beans.validation;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import junit.framework.TestCase;
import net.sf.navel.beans.PropertyBeanHandler;
import net.sf.navel.example.ChildBean;

/**
 * @author thomas
 *
 */
public class PropertyValidatorTest extends TestCase
{
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        Logger root = LogManager.getRootLogger();

        root.removeAllAppenders();

        root.addAppender(new ConsoleAppender(new PatternLayout(
                "%d %-5p [%c] %m%n"), ConsoleAppender.SYSTEM_OUT));
        root.setLevel(Level.DEBUG);
    }

    /**
     * Test method for {@link net.sf.navel.beans.validation.PropertyValidator#validateData(java.beans.BeanInfo)}.
     */
    public void testValidateData()
    {
        Map<String, Object> values = new HashMap<String, Object>(2);

        values.put("parentID", 1L);
        values.put("childID", 2L);

        PropertyBeanHandler<ChildBean> handler = new PropertyBeanHandler<ChildBean>(
                ChildBean.class, values, false);

        ChildBean bean = handler.getProxy();
        
        assertEquals("Inherited properties should work.", 1L, bean.getParentID());
        assertEquals("Declared properties should work.", 2L, bean.getChildID());
    }

}
