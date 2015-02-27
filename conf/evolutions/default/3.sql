# --- !Ups

create table access_log (
  id                        bigint not null,
  user_id                   bigint,
  entity_file_path          varchar(255),
  last_access               timestamp,
  constraint uq_access_log_1 unique (user_id,entity_file_path),
  constraint pk_access_log primary key (id))
;

create sequence access_log_seq;

alter table access_log add constraint fk_access_log_user_1 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_access_log_user_1 on access_log (user_id);

alter table access_log add constraint fk_access_log_entity_2 foreign key (entity_file_path) references share_file_entity (file_path) on delete restrict on update restrict;
create index ix_access_log_entity_2 on access_log (entity_file_path);


# --- !Downs

drop table if exists access_log;

drop sequence if exists access_log_seq;
