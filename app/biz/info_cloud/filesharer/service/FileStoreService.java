package biz.info_cloud.filesharer.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.io.FilenameUtils;

import models.ShareFileEntity;
import models.User;
import play.Logger;
import biz.info_cloud.filesharer.LocalConfig;
import biz.info_cloud.filesharer.service.storage.StorageService;
import biz.info_cloud.filesharer.service.storage.StorageServiceFactory;

public class FileStoreService {
  public StoredFile saveFile(final Optional<User> owner, final File fromFile, final String fromFilename)
      throws IOException {
    // check extension
    String ext = FilenameUtils.getExtension(fromFilename);
    
    String saveFilename = UUID.randomUUID().toString() + "." + ext.toLowerCase(Locale.US);
    StorageService service = getStorageService();
    String storageFilename = service.save(fromFile, saveFilename);
    
    ShareFileEntity entity = new ShareFileEntity();
    entity.filePath = saveFilename;
    entity.originalFilename = FilenameUtils.getName(fromFilename);
    entity.storageFilename = storageFilename;
    owner.ifPresent(user -> entity.owner = user);
    entity.save();
    
    return new StoredFile(entity.filePath);
  }
  
  public StoredFile getStoredFile(final String relativePath) {
    return new StoredFile(relativePath);
  }
  
  public List<ShareFileEntity> getUploadList(User user) {
    return ShareFileEntity.findByOwner(user);
  }
  
  public boolean isOwndFile(final StoredFile storedFile, final User user) {
    if (storedFile == null || storedFile.entity == null) {
      return false;
    }
    
    if (user == null || storedFile.entity.owner == null) {
      return false;
    }
    
    return user.id == storedFile.entity.owner.id;
  }
  
  public void delete(final StoredFile storedFile) {
    delete(storedFile.getKeyPath());
  }
  
  public void delete(final String relativePath) {
    ShareFileEntity entity = ShareFileEntity.findByPath(relativePath);
    if (entity != null) {
      StorageService storageService = getStorageService();
      try {
        storageService.delete(entity.storageFilename);
      } catch (IOException e) {
        Logger.debug(e.toString());
      }
      
      entity.delete();
    }
  }
  
  public void deleteFiles() {
    Logger.info(String.format("Delete shared files started [%s]", LocalDateTime.now().toString()));

    StorageService storageService = getStorageService();
    int keepDate = LocalConfig.getKeepDurationInDays();
    LocalDateTime deleteDate = LocalDateTime.now().minusDays(keepDate);
    List<ShareFileEntity> targetList =
        ShareFileEntity.find.where()
                            .le("createDate", Timestamp.valueOf(deleteDate))
                            .findList();
    Consumer<ShareFileEntity> deleteFileAction =
        target -> {
          try {
            storageService.delete(target.storageFilename);
          } catch (IOException e) {
            Logger.debug(e.toString());
          }
        };
    
    targetList.stream()
              .peek(deleteFileAction)
              .forEach(target -> target.delete());
    
    storageService.cleanup(deleteDate);
    
    Logger.info(String.format("Delete shared files finshed [%s]", LocalDateTime.now().toString()));
  }
  
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
