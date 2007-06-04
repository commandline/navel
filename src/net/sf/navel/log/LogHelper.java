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
package net.sf.navel.log;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;

/**
 * @author cmdln
 *
 */
public class LogHelper
{

    public static void traceError(Logger logger, Throwable cause)
    {
        StringWriter buffer = new StringWriter();
        
        PrintWriter writer = new PrintWriter(buffer);
        
        cause.printStackTrace(writer);
        
        logger.error(buffer.toString());
    }
}
