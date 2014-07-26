package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.avaje.ebean.annotation.CreatedTimestamp;

import play.db.ebean.Model;

@Entity
public class ShareFileEntity extends Model {
  private static final long serialVersionUID = 1L;
  
  @Id
  public String filePath;
  
  @Column(length=4096)
  public String originalFilename;
  
  @CreatedTimestamp
  public Date createDate;
  
  public String toString() {
    return filePath;
  }
  
  public static Finder<String, ShareFileEntity> find =
      new Finder<>(String.class, ShareFileEntity.class);
}
