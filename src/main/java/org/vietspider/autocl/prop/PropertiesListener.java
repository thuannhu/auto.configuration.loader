/***************************************************************************
 * Copyright 2014 by VietSpider - All rights reserved.                *    
 **************************************************************************/
package org.vietspider.autocl.prop;
/**
 *  Author : Nhu Dinh Thuan
 *          Email:nhudinhthuan@gmail.com
 * Jan 12, 2014
 */
import java.util.Properties;

public interface PropertiesListener {

    boolean isFile(String name);

    void update(Properties properties);

    void delete(Properties properties);
}