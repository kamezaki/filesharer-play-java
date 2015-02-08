package biz.info_cloud.filesharer.service.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import play.Logger;
import plugins.S3Plugin;

public class S3Storage implements StorageService {

  @Override
  public InputStream getInputStream(final String storedName) throws IOException {
    try {
      checkS3Plugin();
      S3Object object = getS3().getObject(getBucket(), storedName);
      return object.getObjectContent();
    } catch (AmazonClientException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String save(final File file, final String filename) throws IOException {
    try {
      checkS3Plugin();
      PutObjectRequest request =
          new PutObjectRequest(getBucket(), filename, file);
      request.withCannedAcl(CannedAccessControlList.Private);
      S3Plugin.amazonS3.putObject(request);
    } catch (AmazonClientException e) {
      throw new IOException(e);
    }
    return filename;
  }
  
  @Override
  public void delete(final String storedName) throws IOException {
    try {
      checkS3Plugin();
      getS3().deleteObject(getBucket(), storedName);
    } catch (AmazonClientException e) {
      throw new IOException(e);
    }
  }
  
  @Override
  public void cleanup(final LocalDateTime deleteDate) {
    // do nothing
    return;
  }


  private void checkS3Plugin() {
    if (S3Plugin.amazonS3 == null) {
      Logger.error("amazonS3 is null");
      throw new RuntimeException("amazonS3 was tot initialzed.");
    }
  }
  
  private String getBucket() {
    return S3Plugin.s3Bucket;
  }
  
  private AmazonS3 getS3() {
    return S3Plugin.amazonS3;
  }

}
