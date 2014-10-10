package biz.info_cloud.filesharer.service.storage;

import java.util.Locale;

public class StorageServiceFactory {
  private StorageServiceFactory() {
  }
  
  public static StorageService createStorageService(String type) {
    return createStorageService(StorageType.valueOf(type.toUpperCase(Locale.US)));
  }
  
  public static StorageService createStorageService(StorageType type) {
    StorageService service = null;
    switch (type) {
    case FILE:
      service = new FileStorage();
      break;
    case S3:
      service = new S3Storage();
      break;
    default:
      throw new RuntimeException(String.format("Unknown Storage type [%s]", type.toString()));
    }
    return service;
  }
  
  public enum StorageType {
    FILE, S3;
  }
}
