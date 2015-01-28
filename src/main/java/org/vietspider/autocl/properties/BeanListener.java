/***************************************************************************
 * Copyright 2014 by VietSpider - All rights reserved.                *    
 **************************************************************************/
package org.vietspider.autocl.properties;
/**
 *  Author : Nhu Dinh Thuan
 *          Email:nhudinhthuan@gmail.com
 * Jan 10, 2014
 */
public interface BeanListener<T> {

    void add(T bean);

    void update(T bean);

    void delete(T bean);
}