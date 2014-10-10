package biz.info_cloud.fllesharer.service;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import biz.info_cloud.filesharer.service.FileStoreService.StoredFile;

public class StoredFileTest {
  @ClassRule
  public static MyFakeApplication app = new MyFakeApplication();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  
  @Test
  public void relativeFile() {
    String relativePath = "relativeFile.txt";
    StoredFile storedFile = new StoredFile(relativePath);
    
    assertThat(storedFile.getKeyPath())
        .isNotNull()
        .isEqualTo(relativePath);
  }
  
  @Test
  public void relativeFolderAndFile() {
    String relativePath = "relativeFolder/relativeFile.txt";
    StoredFile storedFile = new StoredFile(relativePath);
    
    assertThat(storedFile.getKeyPath())
        .isNotNull()
        .isEqualTo(relativePath);
  }
  
  @Test
  public void originalFilename() {
    String relativePath = "relativeFile.txt";
    StoredFile storedFile = new StoredFile(relativePath);
    
    assertThat(storedFile.getOriginalFilename())
        .isNotNull()
        .isEqualTo(relativePath);
  }
  
  @Test
  public void originalFolderAndFile() {
    String relativePath = "relativeFolder/relativeFile.txt";
    StoredFile storedFile = new StoredFile(relativePath);
    
    assertThat(storedFile.getOriginalFilename())
        .isNotNull()
        .isEqualTo(relativePath);
  }
}
