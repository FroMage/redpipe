drop table if exists Pages;

create table Pages (
 Id serial, 
 Name varchar(255) unique, 
 Content text,
 primary key(Id));

