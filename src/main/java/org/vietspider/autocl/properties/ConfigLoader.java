/***************************************************************************
 * Copyright 2014 by VietSpider - All rights reserved.                *    
 **************************************************************************/
package org.vietspider.autocl.properties;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 *  Author : Nhu Dinh Thuan
 *          Email:nhudinhthuan@gmail.com
 * Jan 11, 2014
 */
public class ConfigLoader {

  private static ConfigLoader INSTANCE;

  public synchronized final static ConfigLoader getInstance() throws IOException {
    if(INSTANCE != null) return INSTANCE;
    INSTANCE = new ConfigLoader();
    return INSTANCE;
  }

  private Hashtable<String, BeanProperties<?>> map;

  private List<PropertiesListener> listeners;

  private WatchService watchService;
  private boolean flag = true;

  private Logger log;

  private File folder;

  public ConfigLoader() throws IOException {
    this(null);
  }

  public ConfigLoader(final File folder) throws IOException {
    log = Logger.getLogger(getClass());

    map = new Hashtable<String, BeanProperties<?>>();
    watchService = FileSystems.getDefault().newWatchService();
    listeners = new ArrayList<PropertiesListener>();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        flag = false;
        try {
          watchService.close();
        } catch (IOException e) {
          log.error(e);
        }
      }});

    new Thread() {
      public void run() {
        try {
          watch(folder);
        } catch (Exception e) {
          log.error(e);
        }
      }
    }.start();
  }
  
  public File getFolder() { return folder; }

  private void watch(File _folder) throws FileNotFoundException, URISyntaxException, IOException {
    this.folder = _folder;
    if(folder == null) {
      URL url = ConfigLoader.class.getResource("/");
      if(url == null) throw new FileNotFoundException("Context class folder not found!");
      folder = new File(url.toURI());
    }
    //    System.out.println("ConfigLoader.watch--folde="+folder.getAbsolutePath());
    log.info("Mornitor folder: " + folder.getAbsolutePath());

    Path path = Paths.get(folder.getAbsolutePath());
    path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    while(flag) {
      WatchKey watchKey = null;
      try {
        watchKey= watchService.poll(10, TimeUnit.SECONDS);
      } catch (InterruptedException exp) {
        exp.printStackTrace();
        log.error(exp);
      }
      try {
        Thread.sleep(1*1000l);
      } catch(Exception exp) {
        log.error(exp);
      }
//      System.out.println("----  > caal +++ "+ watchKey );
//            log.info("----  > caal +++ "+ watchKey );
      if(watchKey == null) continue;

      processWatchKey(watchKey);
      watchKey.reset();
    }
  }

  private void processWatchKey(WatchKey watchKey) {
    try {
      for (WatchEvent<?> event : watchKey.pollEvents()) {
//                  System.out.println(event.kind() + " : "+ ENTRY_CREATE.equals(event.kind()));
        //          log.info((ENTRY_CREATE.equals(event.kind()) || ENTRY_MODIFY.equals(event.kind())));
        if (ENTRY_CREATE.equals(event.kind())
            || ENTRY_MODIFY.equals(event.kind())) {
          String fileName = event.context().toString();
          BeanProperties<?> container = map.get(fileName);
//          System.out.println("found "+ fileName + " : " + container);
          if(container == null) return;
          load(container.mapper, fileName);
          container =  map.get(fileName);

//          System.out.println(" ------ - >"+ fileName + " : "+ container.beans + " : "+ listeners.size());
          for(int i = 0; i < listeners.size(); i++) {
            PropertiesListener listener = listeners.get(i); 
//            System.out.println(" -- -  > "+ fileName + " :  "+ listener.isFile(fileName));
            if(listener.isFile(fileName)) listener.update(container); 
          }
          return;
        } 

        if (ENTRY_DELETE.equals(event.kind())) {
          String fileName = event.context().toString();
          BeanProperties<?> container = map.get(fileName);
          if(container == null) return;
          //          System.out.println(" ------ - >"+ fileName + " : "+ properties);
          for(int i = 0; i < listeners.size(); i++) {
            PropertiesListener listener = listeners.get(i); 
            if(listener.isFile(fileName)) listener.delete(container); 
          }
          return;
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
      log.error("ERROR", e);
    }
  }

  public synchronized <T> List<T> load(BeanMapper<T, ?, ?> mapper, 
      String name) throws FileNotFoundException, URISyntaxException, IOException {
    return load(mapper, name, null);
  }

  public synchronized <T> List<T> load(BeanMapper<T, ?, ?> mapper, String name, 
      BeanListener<T> listener) throws FileNotFoundException, URISyntaxException, IOException {
    BeanProperties<T> container = loadData(mapper, name, listener);
    return container.beans;  
  }

  public synchronized  Properties load(String name) throws FileNotFoundException, URISyntaxException, IOException {
    return loadData(null, name, null);
  }

  @SuppressWarnings("unchecked")
  private <T> BeanProperties<T> loadData(BeanMapper<T, ?, ?> mapper, 
      String name, BeanListener<T> listener) throws FileNotFoundException, URISyntaxException, IOException {
    File file = new File(folder, name);
    if(!file.exists() || !file.isFile()) {
      URL url = ConfigLoader.class.getResource(name);
      if(url == null) {
        log.error(name + " - File not found", new FileNotFoundException(name));
        BeanProperties<T> container = new BeanProperties<T>(mapper, listener);
        map.put(file.getName(), container);
        return container;
      }
      file = new File(url.toURI());
    }
    log.info(" load properties from  -------------  > "+ file.getAbsolutePath());

    BeanProperties<T> container = (BeanProperties<T>) map.get(file.getName());

    if(container != null) mapper = container.mapper;
    container = load(file, container, mapper, listener);
    map.put(file.getName(), container);
    //    log.info(" --------------------  > mapper " + container.beans.hashCode() + " : "+ container.beans.size()  + " : "+ put +  " : "+ Thread.currentThread().hashCode());
    return container;
  }

  private <T> BeanProperties<T> load(File file, BeanProperties<T> container,
      BeanMapper<T, ?, ?> mapper, BeanListener<T> listener) throws IOException {
    if(container != null) {
      container.clear();
    } else {
      container = new BeanProperties<T>(mapper, listener);
    }

    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(file);
      container.load(inputStream);
    } catch(FileNotFoundException exp) {
      log.info(exp.getMessage());
    } finally {
      if(inputStream != null) inputStream.close();
    }
    return container;
  }

  public synchronized  void addListener(PropertiesListener listener) { listeners.add(listener); }

  public synchronized  void removeListener(PropertiesListener listener) { listeners.remove(listener); }

  public synchronized void addListener(String fileName, BeanListener<?> listener) {
    BeanProperties<?> properties = map.get(fileName);
    if(properties != null) properties.addListener(listener);
  }

  public synchronized void removeListener(String fileName, BeanListener<?> listener) {
    BeanProperties<?> properties = map.get(fileName);
    if(properties != null) properties.removeListener(listener);
  }

}
