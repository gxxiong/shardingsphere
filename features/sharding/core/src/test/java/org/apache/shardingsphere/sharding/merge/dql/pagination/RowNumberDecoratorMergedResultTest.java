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

package org.apache.shardingsphere.sharding.merge.dql.pagination;

import org.apache.shardingsphere.infra.binder.context.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResult;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.resource.ResourceMetaData;
import org.apache.shardingsphere.infra.metadata.database.rule.RuleMetaData;
import org.apache.shardingsphere.infra.session.connection.ConnectionContext;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sharding.merge.dql.ShardingDQLResultMerger;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.BinaryOperationExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.subquery.SubquerySegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.pagination.rownum.NumberLiteralRowNumberValueSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.pagination.top.TopProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SubqueryTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.statement.core.value.identifier.IdentifierValue;
import org.apache.shardingsphere.sql.parser.statement.mysql.dml.MySQLSelectStatement;
import org.apache.shardingsphere.sql.parser.statement.oracle.dml.OracleSelectStatement;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RowNumberDecoratorMergedResultTest {
    
    @Test
    void assertNextForSkipAll() throws SQLException {
        OracleSelectStatement selectStatement = new OracleSelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        WhereSegment whereSegment = mock(WhereSegment.class);
        BinaryOperationExpression binaryOperationExpression = mock(BinaryOperationExpression.class);
        when(binaryOperationExpression.getLeft()).thenReturn(new ColumnSegment(0, 0, new IdentifierValue("row_id")));
        when(binaryOperationExpression.getRight()).thenReturn(new LiteralExpressionSegment(0, 0, Integer.MAX_VALUE));
        when(binaryOperationExpression.getOperator()).thenReturn(">=");
        when(whereSegment.getExpr()).thenReturn(binaryOperationExpression);
        SubqueryTableSegment subqueryTableSegment = mock(SubqueryTableSegment.class);
        SubquerySegment subquerySegment = mock(SubquerySegment.class);
        SelectStatement subSelectStatement = mock(MySQLSelectStatement.class);
        ProjectionsSegment subProjectionsSegment = mock(ProjectionsSegment.class);
        TopProjectionSegment topProjectionSegment = mock(TopProjectionSegment.class);
        when(topProjectionSegment.getAlias()).thenReturn("row_id");
        when(subProjectionsSegment.getProjections()).thenReturn(Collections.singletonList(topProjectionSegment));
        when(subSelectStatement.getProjections()).thenReturn(subProjectionsSegment);
        when(subquerySegment.getSelect()).thenReturn(subSelectStatement);
        when(subqueryTableSegment.getSubquery()).thenReturn(subquerySegment);
        selectStatement.setFrom(subqueryTableSegment);
        selectStatement.setWhere(whereSegment);
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(TypedSPILoader.getService(DatabaseType.class, "Oracle"));
        ShardingSphereDatabase database = mock(ShardingSphereDatabase.class, RETURNS_DEEP_STUBS);
        when(database.getName()).thenReturn("foo_db");
        SelectStatementContext selectStatementContext = new SelectStatementContext(createShardingSphereMetaData(database), null, selectStatement, "foo_db", Collections.emptyList());
        MergedResult actual = resultMerger.merge(Arrays.asList(mockQueryResult(), mockQueryResult(), mockQueryResult(), mockQueryResult()), selectStatementContext, database,
                mock(ConnectionContext.class));
        assertFalse(actual.next());
    }
    
    @Test
    void assertNextWithoutOffsetWithoutRowCount() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(TypedSPILoader.getService(DatabaseType.class, "Oracle"));
        ShardingSphereDatabase database = mock(ShardingSphereDatabase.class, RETURNS_DEEP_STUBS);
        when(database.getName()).thenReturn("foo_db");
        OracleSelectStatement selectStatement = new OracleSelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createShardingSphereMetaData(database), null, selectStatement, "foo_db", Collections.emptyList());
        MergedResult actual = resultMerger.merge(Arrays.asList(mockQueryResult(), mockQueryResult(), mockQueryResult(), mockQueryResult()), selectStatementContext, database,
                mock(ConnectionContext.class));
        for (int i = 0; i < 8; i++) {
            assertTrue(actual.next());
        }
        assertFalse(actual.next());
    }
    
    @Test
    void assertNextForRowCountBoundOpenedFalse() throws SQLException {
        OracleSelectStatement selectStatement = new OracleSelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        WhereSegment whereSegment = mock(WhereSegment.class);
        BinaryOperationExpression binaryOperationExpression = mock(BinaryOperationExpression.class);
        when(binaryOperationExpression.getLeft()).thenReturn(new ColumnSegment(0, 0, new IdentifierValue("row_id")));
        when(binaryOperationExpression.getRight()).thenReturn(new LiteralExpressionSegment(0, 0, 2));
        when(binaryOperationExpression.getOperator()).thenReturn(">=");
        when(whereSegment.getExpr()).thenReturn(binaryOperationExpression);
        SubqueryTableSegment subqueryTableSegment = mock(SubqueryTableSegment.class);
        SubquerySegment subquerySegment = mock(SubquerySegment.class);
        SelectStatement subSelectStatement = mock(MySQLSelectStatement.class);
        ProjectionsSegment subProjectionsSegment = mock(ProjectionsSegment.class);
        TopProjectionSegment topProjectionSegment = mock(TopProjectionSegment.class);
        when(topProjectionSegment.getAlias()).thenReturn("row_id");
        when(topProjectionSegment.getTop()).thenReturn(new NumberLiteralRowNumberValueSegment(0, 0, 4L, false));
        when(subProjectionsSegment.getProjections()).thenReturn(Collections.singletonList(topProjectionSegment));
        when(subSelectStatement.getProjections()).thenReturn(subProjectionsSegment);
        when(subquerySegment.getSelect()).thenReturn(subSelectStatement);
        when(subqueryTableSegment.getSubquery()).thenReturn(subquerySegment);
        selectStatement.setFrom(subqueryTableSegment);
        selectStatement.setWhere(whereSegment);
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(TypedSPILoader.getService(DatabaseType.class, "Oracle"));
        ShardingSphereDatabase database = mock(ShardingSphereDatabase.class, RETURNS_DEEP_STUBS);
        when(database.getName()).thenReturn("foo_db");
        SelectStatementContext selectStatementContext = new SelectStatementContext(createShardingSphereMetaData(database), null, selectStatement, "foo_db", Collections.emptyList());
        MergedResult actual = resultMerger.merge(Arrays.asList(mockQueryResult(), mockQueryResult(), mockQueryResult(), mockQueryResult()), selectStatementContext, database,
                mock(ConnectionContext.class));
        assertTrue(actual.next());
        assertTrue(actual.next());
        assertFalse(actual.next());
    }
    
    @Test
    void assertNextForRowCountBoundOpenedTrue() throws SQLException {
        OracleSelectStatement selectStatement = new OracleSelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        WhereSegment whereSegment = mock(WhereSegment.class);
        BinaryOperationExpression binaryOperationExpression = mock(BinaryOperationExpression.class);
        when(binaryOperationExpression.getLeft()).thenReturn(new ColumnSegment(0, 0, new IdentifierValue("row_id")));
        when(binaryOperationExpression.getRight()).thenReturn(new LiteralExpressionSegment(0, 0, 2));
        when(binaryOperationExpression.getOperator()).thenReturn(">=");
        when(whereSegment.getExpr()).thenReturn(binaryOperationExpression);
        SubqueryTableSegment subqueryTableSegment = mock(SubqueryTableSegment.class);
        SubquerySegment subquerySegment = mock(SubquerySegment.class);
        SelectStatement subSelectStatement = mock(MySQLSelectStatement.class);
        ProjectionsSegment subProjectionsSegment = mock(ProjectionsSegment.class);
        TopProjectionSegment topProjectionSegment = mock(TopProjectionSegment.class);
        when(topProjectionSegment.getAlias()).thenReturn("row_id");
        when(topProjectionSegment.getTop()).thenReturn(new NumberLiteralRowNumberValueSegment(0, 0, 4L, true));
        when(subProjectionsSegment.getProjections()).thenReturn(Collections.singletonList(topProjectionSegment));
        when(subSelectStatement.getProjections()).thenReturn(subProjectionsSegment);
        when(subquerySegment.getSelect()).thenReturn(subSelectStatement);
        when(subqueryTableSegment.getSubquery()).thenReturn(subquerySegment);
        selectStatement.setFrom(subqueryTableSegment);
        selectStatement.setWhere(whereSegment);
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(TypedSPILoader.getService(DatabaseType.class, "Oracle"));
        ShardingSphereDatabase database = mock(ShardingSphereDatabase.class, RETURNS_DEEP_STUBS);
        when(database.getName()).thenReturn("foo_db");
        SelectStatementContext selectStatementContext = new SelectStatementContext(createShardingSphereMetaData(database), null, selectStatement, "foo_db", Collections.emptyList());
        MergedResult actual =
                resultMerger.merge(Arrays.asList(mockQueryResult(), mockQueryResult(), mockQueryResult(), mockQueryResult()), selectStatementContext, database, mock(ConnectionContext.class));
        assertTrue(actual.next());
        assertTrue(actual.next());
        assertTrue(actual.next());
        assertFalse(actual.next());
    }
    
    private ShardingSphereMetaData createShardingSphereMetaData(final ShardingSphereDatabase database) {
        return new ShardingSphereMetaData(Collections.singleton(database), mock(ResourceMetaData.class), mock(RuleMetaData.class), mock(ConfigurationProperties.class));
    }
    
    private QueryResult mockQueryResult() throws SQLException {
        QueryResult result = mock(QueryResult.class, RETURNS_DEEP_STUBS);
        when(result.next()).thenReturn(true, true, false);
        return result;
    }
}
