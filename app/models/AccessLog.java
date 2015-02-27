package models;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import play.data.format.Formats;
import play.db.ebean.Model;

import com.avaje.ebean.ExpressionList;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "entity_file_path"})})
public class AccessLog extends Model {
  private static final long serialVersionUID = 1L;
  
  @Id
  public Long id;
  
  @ManyToOne
  public User user;
  
  @ManyToOne
  public ShareFileEntity entity;

  @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
  public Date lastAccess;

  public static final Finder<Long, AccessLog> find =
      new Finder<>(Long.class, AccessLog.class);
  
  private static ExpressionList<AccessLog> getUserFind(final User user) {
    return find.fetch("entity").fetch("entity.owner").where().eq("user", user);
  }
  
  public static List<AccessLog> findByUser(final User user) {
    if (user == null) {
      return Collections.emptyList();
    }
    
    return getUserFind(user).orderBy("lastAccess desc")
                            .findList();
  }
  
  private static ExpressionList<AccessLog> getEntityFind(final ShareFileEntity entity) {
    return find.where().eq("entity", entity);
  }
  
  public static List<AccessLog> findByEntity(final ShareFileEntity entity) {
    if (entity == null) {
      return Collections.emptyList();
    }
    return getEntityFind(entity).findList();
  }
  
  private static ExpressionList<AccessLog> getUserAndEntity(
      final User user, final ShareFileEntity entity) {
    return find.where().eq("user", user)
                       .eq("entity", entity);
  }
  
  public static AccessLog findByUserAndEntity(
      final User user, final ShareFileEntity entity) {
    return getUserAndEntity(user, entity).findUnique();
  }
  
  public static AccessLog create(final User user, final ShareFileEntity entity) {
    final AccessLog accessLog = new AccessLog();
    accessLog.user = user;
    accessLog.entity = entity;
    accessLog.lastAccess = new Date();
    accessLog.save();
    return accessLog;
  }
  
  public static AccessLog updateAccess(
      final User user, final ShareFileEntity entity) {
    if (user == null || entity == null) {
      return null;
    }
    
    final AccessLog accessLog = findByUserAndEntity(user, entity);
    if (accessLog == null) {
      return create(user, entity);
    }
    accessLog.lastAccess = new Date();
    accessLog.update();
    return accessLog;
  }
  
  private static void deleteList(List<AccessLog> list) {
    for (AccessLog log : list) {
      log.delete();
    }
  }
  
  public static void deleteByEntity(final ShareFileEntity entity) {
    final List<AccessLog> list = findByEntity(entity);
    deleteList(list);
  }
  
  public static void deleteByUser(final User user) {
    final List<AccessLog> list = findByUser(user);
    deleteList(list);
  }
}
