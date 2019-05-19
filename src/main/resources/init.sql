create table Profiles (
    profile_name varchar primary key,
    password varchar not null,
    last_seen timestamp
);

create table Actions (
    id bigint primary key,
    profile_name varchar not null,
    action_timestamp timestamp not null,
    action_type varchar not null
);

create table ActionsType (
    action_type varchar primary key,
    action_descr varchar not null
);

alter table Actions add constraint profiles_fk foreign key (profile_name) references Profiles(profile_name);

alter table Actions add constraint actionstype_fk foreign key (action_type) references ActionsType(action_type);

create sequence actions_sq;

insert into ActionsType (action_type, action_descr)
    values ('CREATED', 'profile-created'), ('LOGIN', 'login'), ('PASSWORD_CHANGED', 'change-password');