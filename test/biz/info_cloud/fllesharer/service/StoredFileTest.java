package biz.info_cloud.fllesharer.service;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;

import models.ShareFileEntity;

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
  public void existsByNonExistingFile() {
    String relativePath = "relativeFile.txt";
    StoredFile storedFile = new StoredFile(relativePath);
    
    assertThat(storedFile.exists()).isEqualTo(false);
  }
  
  @Test
  public void existsByExistingFile() throws Exception {
    String relativePath = "relativeFile.txt";
    temporaryFolder.newFile(relativePath);
    StoredFile storedFile = new StoredFile(relativePath);
    
    assertThat(storedFile.exists()).isEqualTo(true);
  }
  
  @Test
  public void existsByNonExistentFolderAndFile() {
    String relativePath = "relativeFolder/relativeFile.txt";
    StoredFile storedFile = new StoredFile(relativePath);
    
    assertThat(storedFile.exists()).isEqualTo(false);
  }
  
  @Test
  public void existsByExistentFolderAndFile() throws Exception {
    String folder = "relativeFolder";
    String filename = "relativeFile.txt"; 
    String relativePath = String.format("%s/%s", folder, filename);
    
    File relativeFolder = temporaryFolder.newFolder(folder);
    File relativeFile = new File(relativeFolder, filename);
    relativeFile.createNewFile();
    
    StoredFile storedFile = new StoredFile(relativePath);
    
    assertThat(storedFile.exists()).isEqualTo(true);
  }
  
  @Test
  public void originalFilenameByNonEntity() throws Exception {
    String folder = "relativeFolder";
    String filename = "relativeFile.txt"; 
    String relativePath = String.format("%s/%s", folder, filename);
    
    File relativeFolder = temporaryFolder.newFolder(folder);
    File relativeFile = new File(relativeFolder, filename);
    relativeFile.createNewFile();
    
    StoredFile storedFile = new StoredFile(relativePath);

    assertThat(storedFile.getOriginalFilename())
        .isNotNull()
        .isEqualTo(relativePath);
  }
  
  @Test
  public void originalFilenameByExistEntity() throws Exception {
    String folder = "relativeFolder";
    String filename = "relativeFileWithOriginal.txt"; 
    String originalName = "originalFile.TXT";
    String relativePath = String.format("%s/%s", folder, filename);
    
    File relativeFolder = temporaryFolder.newFolder(folder);
    File relativeFile = new File(relativeFolder, filename);
    relativeFile.createNewFile();
    
    ShareFileEntity entity = new ShareFileEntity();
    entity.filePath = filename;
    entity.originalFilename = originalName;
    entity.save();
    
    StoredFile storedFile = new StoredFile(relativePath);

    assertThat(storedFile.getOriginalFilename())
        .isNotNull()
        .isEqualTo(originalName);
    
  }
}
