/***************************************************************************
 * Copyright 2014 by VietSpider - All rights reserved.                *    
 **************************************************************************/
package org.vietspider.autocl.properties;

/**
 *  Author : Nhu Dinh Thuan
 *          Email:nhudinhthuan@gmail.com
 * Jan 10, 2014
 */
 public interface BeanMapper<T,K,V> {

    public K getKey(T bean);

    public V getValue(T bean);

    public T map(T bean, String key, String value);

  }