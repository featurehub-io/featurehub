-- apply changes
alter table fh_person add column password_alg varchar(60) default 'PBKDF2WithHmacSHA1' not null;

