package com.store.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider extends  AutoCloseable{
    Connection getConnection() throws SQLException;

    @Override
    default void close() {}

}
