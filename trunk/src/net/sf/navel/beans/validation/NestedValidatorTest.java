package net.sf.navel.beans.validation;

import static net.sf.navel.beans.BeanManipulator.describe;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.sf.navel.beans.PropertyBeanHandler;
import net.sf.navel.example.NestedBean;
import net.sf.navel.example.TypesBean;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class NestedValidatorTest extends TestCase
{
    
    private static final Logger LOGGER = LogManager.getLogger(NestedValidatorTest.class);

    public void testInit() throws Exception
    {
        Map<String,Object> values = new HashMap<String,Object>();
        values.put("long", new Long(63));
        values.put("short", new Short((short)6));
        values.put("nested.long", new Long(42));
        values.put("nested.short", new Short((short)4));
        
        NestedBean bean = new PropertyBeanHandler<NestedBean>(NestedBean.class, values, true).getProxy();
        
        LOGGER.debug(bean);
        
        assertEquals("Regular property should be set.", 63L, bean.getLong());
        assertNotNull("Parent property should be set.", bean.getNested());
        assertEquals("Nested property should be set.", 42L, bean.getNested().getLong());
    }
    
    public void testNestedTooDeep()
    {
        Map<String,Object> values = new HashMap<String,Object>();
        values.put("long", new Long(63));
        values.put("nested.long", new Long(42));
        values.put("nested.too.long", new Long(42));

        try
        {
            new PropertyBeanHandler<NestedBean>(NestedBean.class, values, true).getProxy();
            
            fail("Should not have been able to construct with an overly deep property name.");
        }
        catch (Exception e)
        {
            LOGGER.debug("Caught bad value.");
        }
        
    }
    
    public void testFlatten() throws Exception
    {
        NestedBean bean = new PropertyBeanHandler<NestedBean>(NestedBean.class).getProxy();
        
        bean.setLong(63L);
        bean.setShort((short)6);
        bean.setNested(new PropertyBeanHandler<TypesBean>(TypesBean.class).getProxy());
        bean.getNested().setLong(42L);
        bean.getNested().setShort((short)4);
        
        Map<String,Object> values = describe(bean, true);
        
        LOGGER.debug(values);
        
        assertEquals("Should have correct number of properties.", 10, values.size());
        
        values = describe(bean);

        LOGGER.debug(values);
        
        assertEquals("Should have correct number of properties.", 9, values.size());
    }
}
