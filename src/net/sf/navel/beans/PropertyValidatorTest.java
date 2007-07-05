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

import net.sf.navel.example.ChildBean;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author thomas
 * 
 */
public class PropertyValidatorTest
{

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
     * Test method for
     * {@link net.sf.navel.beans.PropertyValidator#validateData(java.beans.BeanInfo)}.
     */
    @Test
    public void testValidateData()
    {
        Map<String, Object> values = new HashMap<String, Object>(2);

        values.put("parentID", 1L);
        values.put("childID", 2L);

        ChildBean bean = ProxyFactory.createAs(ChildBean.class, values);

        Assert.assertEquals(1L, bean.getParentID(),
                "Inherited properties should work.");
        Assert.assertEquals(2L, bean.getChildID(),
                "Declared properties should work.");
    }

}
