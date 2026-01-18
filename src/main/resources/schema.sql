create table users (
	id bigserial primary key,
	login varchar(250) not null,
	password_hash varchar(250) not null
);

create table user_sessions (
	user_id bigint not null,
	access_token varchar(150) not null,

	constraint fk_user_id foreign key (user_id) references users (id)
);

create table files_data (
	id bigserial primary key,
	user_id bigint not null,
	name varchar(250) not null,
	local_name varchar(250) not null,
	checksum varchar(128) not null,
	size bigint not null,

	constraint fk_user_id foreign key (user_id) references users (id)
);
