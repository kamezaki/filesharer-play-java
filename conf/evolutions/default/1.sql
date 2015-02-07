# --- !Ups

create table linked_account (
  id                        bigint not null,
  user_id                   bigint,
  provider_user_id          varchar(255),
  provider_key              varchar(255),
  constraint pk_linked_account primary key (id))
;

create table share_file_entity (
  file_path                 varchar(255) not null,
  original_filename         varchar(4096),
  storage_filename          varchar(4096),
  owner_id                  bigint,
  create_date               timestamp not null,
  constraint pk_share_file_entity primary key (file_path))
;

create table token_action (
  id                        bigint not null,
  token                     varchar(255),
  target_user_id            bigint,
  type                      varchar(2),
  created                   timestamp,
  expires                   timestamp,
  constraint ck_token_action_type check (type in ('PR','EV')),
  constraint uq_token_action_token unique (token),
  constraint pk_token_action primary key (id))
;

create table users (
  id                        bigint not null,
  email                     varchar(255),
  name                      varchar(255),
  last_login                timestamp,
  active                    boolean,
  email_validated           boolean,
  constraint pk_users primary key (id))
;

create sequence linked_account_seq;

create sequence share_file_entity_seq;

create sequence token_action_seq;

create sequence users_seq;

alter table linked_account add constraint fk_linked_account_user_1 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_linked_account_user_1 on linked_account (user_id);
alter table share_file_entity add constraint fk_share_file_entity_owner_2 foreign key (owner_id) references users (id) on delete restrict on update restrict;
create index ix_share_file_entity_owner_2 on share_file_entity (owner_id);
alter table token_action add constraint fk_token_action_targetUser_3 foreign key (target_user_id) references users (id) on delete restrict on update restrict;
create index ix_token_action_targetUser_3 on token_action (target_user_id);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists linked_account;

drop table if exists share_file_entity;

drop table if exists token_action;

drop table if exists users;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists linked_account_seq;

drop sequence if exists share_file_entity_seq;

drop sequence if exists token_action_seq;

drop sequence if exists users_seq;

