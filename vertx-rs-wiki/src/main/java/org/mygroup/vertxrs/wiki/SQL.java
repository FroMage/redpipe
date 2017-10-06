package org.mygroup.vertxrs.wiki;

public class SQL {
	static final String SQL_CREATE_PAGES_TABLE = "create table if not exists Pages (Id integer identity primary key, Name varchar(255) unique, Content clob)";
	static final String SQL_GET_PAGE = "select Id, Content from Pages where Name = ?";
	static final String SQL_GET_PAGE_BY_ID = "select Name, Content from Pages where Id = ?";
	static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
	static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
	static final String SQL_ALL_PAGES = "select Name from Pages";
	static final String SQL_ALL_PAGES_DATA = "select Name, Id, Content from Pages";
	static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";
}
