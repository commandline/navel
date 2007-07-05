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

import net.sf.navel.beans.DelegationTarget;
import net.sf.navel.beans.PropertyValues;

import org.apache.log4j.Logger;

/**
 * Example delegate that implements a functinal interface and can be
 * instantiated and used with DelegateBeanHandler. Must implement
 * DelegationTarget, but the constructor of DelegateBeanHandler won't accept
 * anything else, so that shouldn't be a problem. May implement as many delegate
 * interfaces as desired, construction of the DelegateBeanHandler just makes
 * sure that there is some DelegationHandler for each extra interface, whether
 * there is just one DelegationHandler or one per interface or some other
 * arbitrary ratio doesn't matter.
 * 
 * @author cmdln
 */
public class AnotherBadDelegatedImpl implements DelegationTarget
{

    private static final Logger LOGGER = Logger
            .getLogger(AnotherBadDelegatedImpl.class);

    private transient PropertyValues values;

    public Class<?> getDelegatingInterface()
    {
        return Delegated.class;
    }

    public void setPropertyValues(PropertyValues values)
    {
        this.values = values;
    }

    public void doThis(Integer foo, Integer bar)
    {
        try
        {
            values.put("writeOnly", foo);
            values.put("readWrite", bar);
        }
        catch (Exception e)
        {
            LOGGER.error(e);
        }
    }

    public Long doThat(Integer foo, Integer bar)
    {
        doThis(foo, bar);

        return new Long(foo.intValue() + bar.intValue());
    }
}
