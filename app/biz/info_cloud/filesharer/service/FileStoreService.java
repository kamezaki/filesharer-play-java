package biz.info_cloud.filesharer.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import models.ShareFileEntity;
import play.Logger;
import biz.info_cloud.filesharer.LocalConfig;
import biz.info_cloud.filesharer.service.storage.StorageService;
import biz.info_cloud.filesharer.service.storage.StorageServiceFactory;

public class FileStoreService {
  public StoredFile saveFile(final File fromFile, final String fromFilename)
      throws IOException {
    // check extension
    int index = fromFilename.lastIndexOf(".");
    String ext = "";
    if (index >= 0) {
      ext = fromFilename.substring(index);
    }
    
    String saveFilename = UUID.randomUUID().toString() + ext;
    StorageService service = getStorageService();
    String storageFilename = service.save(fromFile, saveFilename);
    
    ShareFileEntity entity = new ShareFileEntity();
    entity.filePath = saveFilename;
    entity.originalFilename = fromFilename;
    entity.storageFilename = storageFilename;
    entity.save();
    
    return new StoredFile(entity.filePath);
  }
  
  public StoredFile getStoredFile(final String relativePath) {
    return new StoredFile(relativePath);
  }
  
  public void deleteFiles() {
    Logger.info(String.format("Delete shared files start [%s]", new Date().toString()));
    
//    File topDir = new File(storeTopDirName);
//    if (!topDir.exists() || !topDir.isDirectory()) {
//      Logger.debug(String.format("Directory [%s] not found" , storeTopDirName));
//      return;
//    }
    
//    File[] list = topDir.listFiles((file, name) -> file.isDirectory());
//    if (list == null || list.length < 1) {
//      Logger.debug(
//          String.format("could not find any directory in %s", storeTopDirName));
//      return;
//    }
//    int sum = 
//    Arrays.stream(list)
//          .parallel()
//          .filter(f -> isDeleteTarget(f))
//          .mapToInt(f -> deleteFile(f))
//          .sum();
//    Logger.info(String.format(
//        "Delete shared files finished [%s] %d files deleted", new Date().toString(), sum));
  }
  
//  private int deleteFile(File file) {
//    if (file.isDirectory()) {
//      int num = 0;
//      File[] list = file.listFiles();
//      if (list != null && list.length > 0) {
//        num = Arrays.stream(list)
//                    .parallel()
//                    .mapToInt(f -> deleteFile(f))
//                    .sum();
//      }
//      file.delete();
//      return num;
//    } else {
//      file.delete();
//      ShareFileEntity entity = ShareFileEntity.find.byId(file.getName());
//      if (entity != null) {
//        entity.delete();
//      }
//      return 1;
//    }
//  }
  
//  private boolean isDeleteTarget(File dir) {
//    if (!dir.isDirectory()) {
//      return false;
//    }
//
//    int keepDate = LocalConfig.getKeepDurationInDays();
//    Calendar cal = Calendar.getInstance();
//    cal.add(Calendar.DATE, -keepDate);
//    String oldestKeepDirectoryName = getStoreRelativeDirectory(cal);
//    if (dir.getName().compareTo(oldestKeepDirectoryName) < 0) {
//      return true;
//    }
//    return false;
//  }
  
  private static StorageService getStorageService() {
    return StorageServiceFactory.createStorageService(LocalConfig.getStorageType());
  }
  
  public static class StoredFile {
    private ShareFileEntity entity;
    private String keyPath;
    
    public StoredFile(final String key) {
      this(key, null);
    }
    
    public StoredFile(final String key, ShareFileEntity entity) {
      this.keyPath = key;
      this.entity = entity;
    }
    
    public String getKeyPath() {
      return keyPath;
    }
    
    public boolean exists() {
      if (getEntity() != null) {
        return true;
      } else {
        return false;
      }
    }
    
    public String getOriginalFilename() {
      ShareFileEntity entity = getEntity();
      if (entity == null) {
        Logger.debug(String.format("Not found in database %s", keyPath));
      }
      return (entity != null) ? entity.originalFilename : keyPath;
    }
    
    public InputStream getInputStream() throws IOException {
      StorageService service = FileStoreService.getStorageService();
      ShareFileEntity entity = getEntity();
      return service.getInputStream(entity.storageFilename);
    }
    
    private synchronized ShareFileEntity getEntity() {
      if (entity == null) {
        entity = ShareFileEntity.find.byId(keyPath);
      }
      return entity;
    }
  }
}
