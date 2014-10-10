# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table share_file_entity (
  file_path                 varchar(255) not null,
  original_filename         varchar(4096),
  storage_filename          varchar(4096),
  create_date               timestamp not null,
  constraint pk_share_file_entity primary key (file_path))
;

create sequence share_file_entity_seq;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists share_file_entity;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists share_file_entity_seq;

