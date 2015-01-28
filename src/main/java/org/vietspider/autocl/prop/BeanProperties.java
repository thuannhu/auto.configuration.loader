/***************************************************************************
 * Copyright 2014 by VietSpider - All rights reserved.                *    
 **************************************************************************/
package org.vietspider.autocl.prop;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 *  Author : Nhu Dinh Thuan
 *          Email:Email:nhudinhthuan@gmail.com
 * Jan 11, 2014
 */
@SuppressWarnings("serial")
public class BeanProperties<T> extends Properties {

  List<T> beans;

  BeanMapper<T, ?, ?> mapper;

  private List<BeanListener<T>> listeners;

  public BeanProperties(BeanMapper<T, ?, ?> mapper, BeanListener<T> listener) {
    this.mapper = mapper;
    if(mapper == null) return;
    listeners = new ArrayList<BeanListener<T>>();
    if(listener != null) listeners.add(listener);
    beans = new ArrayList<T>();
  }
  
  @SuppressWarnings("unchecked")
  public void addListener(BeanListener<?> listener) { listeners.add((BeanListener<T>)listener); }

  public void removeListener(BeanListener<?> listener) { listeners.remove(listener); }

  @Override
  public synchronized Object put(Object _key, Object _value) {
    Object _return = super.put(_key, _value);
    
//    System.out.println(" -----  mapppasda d ---  >"+ mapper);
    
    if(mapper == null) return _return;
    
    String key = (String) _key;
    String value = (String) _value;
    
    for(int i = 0; i < beans.size(); i++) {
      T bean = beans.get(i);
      if(!key.equals(mapper.getKey(bean))) continue;
//      System.out.println("key "+ key + " : " + mapper.getKey(bean) + " : " + key.equals(mapper.getKey(bean)));
      if(value.equals(mapper.getValue(bean))) return bean;
//      System.out.println("value "+ value + " : " + mapper.getValue(bean) + " : " + value.equals(mapper.getValue(bean)));
      mapper.map(bean, key, value);
      for(int j = 0; j < listeners.size(); j++) {
        listeners.get(j).update(bean);
      }
      return bean;
    }
    
    T bean = mapper.map(null, key, value);
    beans.add(bean);
//    System.out.println(" asdasdasd --- asdd new s -----  >"+ bean);
    for(int j = 0; j < listeners.size(); j++) {
      listeners.get(j).add(bean);
    }
    return bean;
  }

  @Override
  public synchronized void load(InputStream inStream) throws IOException {
    super.load(inStream);
    if(mapper == null) return;
    Iterator<T> iterator = beans.iterator();
    while(iterator.hasNext()) {
      T bean = iterator.next();
      Object key = mapper.getKey(bean);
      Object value = get(key);
      if(value != null) continue;
      iterator.remove();
      for(int j = 0; j < listeners.size(); j++) {
        listeners.get(j).delete(bean);
      }
    }

  }


}
