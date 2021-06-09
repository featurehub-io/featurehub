-- apply changes
alter table fh_person add password_alg nvarchar(60) default 'PBKDF2WithHmacSHA1' not null;

