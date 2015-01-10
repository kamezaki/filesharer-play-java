package biz.info_cloud.fllesharer.service;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import models.ShareFileEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import biz.info_cloud.filesharer.service.FileStoreService;
import biz.info_cloud.filesharer.service.FileStoreService.StoredFile;

public class FileStoreServiceTest {
  @ClassRule
  public static MyFakeApplication app = new MyFakeApplication();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  
  private File sourceFile;

  @Before
  public void setUp() throws Exception {
    String relativePath = "source.txt";
    sourceFile = temporaryFolder.newFile(relativePath);
  }
  
  @After
  public void tearDown() {
    if (sourceFile != null) {
      sourceFile.delete();
    }
  }
  
  @Test
  public void constructWithStringPath() {
    FileStoreService service = new FileStoreService();
    String testFolder = "testFolderName";
    
    StoredFile storedFile = service.getStoredFile(testFolder);
    assertThat(storedFile).isNotNull();
  }
  
  @Test
  public void saveFile() throws Exception {
    FileStoreService service = new FileStoreService();
    StoredFile storedFile = service.saveFile(
        Optional.empty(), sourceFile, sourceFile.getName());
    
    assertThat(storedFile).isNotNull();
    assertThat(storedFile.getOriginalFilename())
      .isNotNull()
      .isEqualTo(sourceFile.getName());
    
    String relativePath = storedFile.getKeyPath();
    int index = relativePath.lastIndexOf('/');
    String uuid = relativePath.substring(index >= 0 ? index + 1 : 0);
    ShareFileEntity entity = ShareFileEntity.find.byId(uuid);
    assertThat(entity).isNotNull();
  }
  
  @Test
  public void saveFileWithUpperCaseExtension() throws Exception {
    FileStoreService service = new FileStoreService();
    int position = sourceFile.getName().lastIndexOf(".");
    String name = sourceFile.getName();
    String ext = "";
    if (position >= 0) {
      ext = name.substring(position);
      name = name.substring(0, position) + ext.toUpperCase();
    }
    StoredFile storedFile = service.saveFile(
        Optional.empty(), sourceFile, name);
    
    assertThat(storedFile).isNotNull();
    assertThat(storedFile.getOriginalFilename())
      .isNotNull()
      .isEqualTo(name);
    
    String relativePath = storedFile.getKeyPath();
    position = relativePath.lastIndexOf(".");
    // should have extension
    assertThat(position).isGreaterThanOrEqualTo(0);
    // extension should be lower case
    assertThat(relativePath.substring(position))
      .isEqualTo(ext);
    
    int index = relativePath.lastIndexOf('/');
    String uuid = relativePath.substring(index >= 0 ? index + 1 : 0);
    ShareFileEntity entity = ShareFileEntity.find.byId(uuid);
    assertThat(entity).isNotNull();
  }
  
  @Test(expected=IOException.class)
  public void saveFileByNonExistingFile() throws Exception {
    String filename = "relativeFile.txt";
    FileStoreService service = new FileStoreService();
    File file = new File(temporaryFolder.getRoot(), filename);
    
    service.saveFile(
        Optional.empty(), file, filename);
  }
}
