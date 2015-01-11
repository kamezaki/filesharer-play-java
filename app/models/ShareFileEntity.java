package models;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.db.ebean.Model;

import com.avaje.ebean.ExpressionList;
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
  
  @ManyToOne(optional = true)
  public User owner;
  
  @CreatedTimestamp
  public Timestamp createDate;

  public static Finder<String, ShareFileEntity> find =
      new Finder<>(String.class, ShareFileEntity.class);
  
  public static List<ShareFileEntity> findByOwner(final User owner) {
    if (owner == null) {
      return Collections.emptyList();
    }
    return getOwnerFind(owner).orderBy("createDate desc")
                              .findList();
  }
  
  public static ExpressionList<ShareFileEntity> getOwnerFind(final User owner) {
    return find.where().eq("owner", owner);
  }
  
  @Override
  public String toString() {
    return filePath;
  }
}
