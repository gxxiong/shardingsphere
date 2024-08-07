/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.driver.executor.callback.execute;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.ConnectionMode;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.SQLExecutorExceptionHandler;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutorCallback;
import org.apache.shardingsphere.infra.executor.sql.prepare.driver.jdbc.JDBCDriverType;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Execute callback factory.
 */
@RequiredArgsConstructor
public final class ExecuteCallbackFactory {
    
    private final String jdbcDriverType;
    
    /**
     * Create new instance of execute callback.
     *
     * @param database database
     * @param sqlStatement SQL statement
     * @param executeCallback statement execute callback
     * @return created instance
     */
    public JDBCExecutorCallback<Boolean> newInstance(final ShardingSphereDatabase database, final StatementExecuteCallback executeCallback, final SQLStatement sqlStatement) {
        return new JDBCExecutorCallback<Boolean>(database.getProtocolType(), database.getResourceMetaData(), sqlStatement, SQLExecutorExceptionHandler.isExceptionThrown()) {
            
            @Override
            protected Boolean executeSQL(final String sql, final Statement statement, final ConnectionMode connectionMode, final DatabaseType storageType) throws SQLException {
                return JDBCDriverType.STATEMENT.equals(jdbcDriverType) ? executeCallback.execute(sql, statement) : ((PreparedStatement) statement).execute();
            }
            
            @Override
            protected Optional<Boolean> getSaneResult(final SQLStatement sqlStatement1, final SQLException ex) {
                return Optional.empty();
            }
        };
    }
}
