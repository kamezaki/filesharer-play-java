package models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.ebean.Model;

import com.avaje.ebean.annotation.CreatedTimestamp;

@Entity
public class ShareFileEntity extends Model {
  private static final long serialVersionUID = 1L;
  
  @Id
  public String filePath;
  
  @Column(length=4096)
  public String originalFilename;
  
  @Column(length=4096)
  public String storageFilename;
  
  @CreatedTimestamp
  public Timestamp createDate;
  
  @Override
  public void delete() {
    super.delete();
  }

  @Override
  public String toString() {
    return filePath;
  }
  
  public static Finder<String, ShareFileEntity> find =
      new Finder<>(String.class, ShareFileEntity.class);
}
