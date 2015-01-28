/***************************************************************************
 * Copyright 2014 by VietSpider - All rights reserved.                *    
 **************************************************************************/
package org.vietspider.autocl.prop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.junit.Test;
import org.vietspider.autocl.prop.BeanListener;
import org.vietspider.autocl.prop.BeanMapper;
import org.vietspider.autocl.prop.ConfigLoader;

/**
 *  Author : Nhu Dinh Thuan
 *          Email:nhudinhthuan@gmail.com
 * Jan 12, 2014
 */
public class BeanLoaderTest extends TestCase {

  @Test
  public void test() throws Exception {
    ConfigLoader loader = ConfigLoader.getInstance();
    BeanListener<Property> beanListener = new BeanListener<Property>() {

      @Override
      public void add(Property bean) {
        System.out.println(" add "+ bean.getKey() + " : "+ bean.getValue());
      }

      @Override
      public void update(Property bean) {
        System.out.println(" update "+ bean.getKey() + " : "+ bean.getValue());
      }

      @Override
      public void delete(Property bean) {
        System.out.println(" delete "+ bean.getKey() + " : "+ bean.getValue());
      }
    };


    BeanMapper<Property, String, String> mapper = new BeanMapper<Property, String, String>() {

      public String getKey(Property bean) { return bean.getKey(); }

      public String getValue(Property bean) { return bean.getValue(); }

      public Property map(Property bean, String key, String value) {
        if(bean == null) bean = new Property();
        bean.setKey(key);
        bean.setValue(value);
        return bean;
      }
    };

    final List<Property> beans = loader.load(mapper, "test.properties", beanListener);
    assertEquals(beans.size(), 3);

    new Thread() {
      public void run() {
        File file = null;
        try {
          URL url = BeanLoaderTest.class.getResource("/test.properties");
          file = new File(url.toURI());
          
          putProperty(file, "key4", "value4");
          Thread.sleep(3000);
          assertEquals(4, beans.size());
          
          deleteProperty(file, "key4");
          Thread.sleep(3000);
          assertEquals(3, beans.size());
          
          
          putProperty(file, "key2", "new value");
          Thread.sleep(3000);
          assertEquals("new value", beans.get(1).getValue());
        } catch (Exception exp) {
          exp.printStackTrace();
        } 
      }
    }.start();

    try {
      Thread.sleep(12*1000l);;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  private void deleteProperty(File file, final String key) throws IOException {
    writeProperty(file, new WriteAction() {
      @Override public void write(Properties properties) {
        properties.remove(key);
      }
    });
  }
  
  private void putProperty(File file, final String key, final String value) throws IOException {
    writeProperty(file, new WriteAction() {
      @Override public void write(Properties properties) {
        properties.put(key, value);
      }
    });
  }
  
  private void writeProperty(File file, WriteAction action) throws IOException {
    FileOutputStream fileOutputStream = null;
    FileInputStream fileInputStream = null;

    Properties properties = new Properties();
    try {
      fileInputStream = new FileInputStream(file);
      properties.load(fileInputStream);

      action.write(properties);

      fileOutputStream = new FileOutputStream(file);
      properties.store(fileOutputStream, null);
    } finally {
      try {
        if(fileOutputStream != null) fileOutputStream.close();
      } catch (IOException exp) {
        exp.printStackTrace();
      }
      try {
        if(fileInputStream != null) fileInputStream.close();
      } catch (IOException exp) {
        exp.printStackTrace();
      }
    }
  }

  private interface WriteAction {
    
    public void write(Properties properties) ;
    
  }

}
