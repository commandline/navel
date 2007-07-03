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
package net.sf.navel.example;

import net.sf.navel.beans.MethodHandler;
import net.sf.navel.beans.DelegationTarget;


/**
 * Implements the bad bean interface so we can do some concrete testing of the
 * BeanManipulator.
 *
 * @author cmdln
 * @version $Revision: 1.3 $, $Date: 2005/09/02 21:31:19 $
 */
public class BadBeanImpl implements BadPropertyBean, DelegationTarget
{

    private static final long serialVersionUID = -2188450661346952639L;

    public void setDelegationSource(MethodHandler handler)
    {
        // purposeful does nothing
    }

    public void setFoo(String value1, String value2)
    {
        // purposeful does nothing
    }

    public int getFoo(String value1, String value2)
    {
        return 0;
    }

    public String setBar(String value1)
    {
        return null;
    }

    public void getBar()
    {
        // purposeful does nothing
    }

    public String isNotBooleanAlt()
    {
        return null;
    }

    public Boolean isWrapperAlt()
    {
        return Boolean.FALSE;
    }
}
