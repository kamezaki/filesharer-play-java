package biz.info_cloud.fllesharer.service;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

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
  
  @Test(expected=NullPointerException.class)
  public void constructWithNullArgument() {
    new FileStoreService(null);
    fail("Constructor should throw exeption when argument is null");
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void consructWithEmtpyStringArgument() {
    new FileStoreService("");
    fail("Constructor should throw exeption when argument is empty string");
  }
  
  @Test
  public void constructWithStringPath() {
    FileStoreService service = new FileStoreService(temporaryFolder.getRoot().getAbsolutePath());
    String testFolder = "testFolderName";
    
    StoredFile storedFile = service.getStoredFile(testFolder);
    assertThat(storedFile).isNotNull();
    assertThat(storedFile.getAbsolutePath())
        .startsWith(temporaryFolder.getRoot().getAbsolutePath());
  }
  
  @Test
  public void saveFile() throws Exception {
    FileStoreService service = new FileStoreService(temporaryFolder.getRoot().getAbsolutePath());
    StoredFile storedFile = service.saveFile(sourceFile, sourceFile.getName());
    
    assertThat(storedFile).isNotNull();
    assertThat(storedFile.getOriginalFilename()).isNotNull()
                                                .isEqualTo(sourceFile.getName());
    
    String relativePath = storedFile.getRelativePath();
    int index = relativePath.lastIndexOf('/');
    String uuid = relativePath.substring(index >= 0 ? index + 1 : 0);
    ShareFileEntity entity = ShareFileEntity.find.byId(uuid);
    assertThat(entity).isNotNull();
  }
  
  @Test(expected=IOException.class)
  public void saveFileByNonExistingFile() throws Exception {
    String filename = "relativeFile.txt";
    FileStoreService service = new FileStoreService(temporaryFolder.getRoot().getAbsolutePath());
    File file = new File(temporaryFolder.getRoot(), filename);
    
    service.saveFile(file, filename);
  }
}
