package models;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.ebean.Model;
import be.objectify.deadbolt.core.models.Role;

@Entity
public class SecurityRole extends Model implements Role {
  private static final long serialVersionUID = 1L;

  @Id
  public Long id;
  
  public String roleName;
  
  public static final Finder<Long, SecurityRole> find =
      new Finder<>(Long.class, SecurityRole.class);
  
  @Override
  public String getName() {
    return roleName;
  }

  public static SecurityRole findByRoleName(final String roleName) {
    return find.where().eq("roleName", roleName).findUnique();
  }
}
