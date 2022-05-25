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

package org.apache.shardingsphere.infra.metadata.rule;

import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class ShardingSphereRuleMetaDataTest {
    
    private final ShardingSphereRuleMetaData ruleMetaData = new ShardingSphereRuleMetaData(
            Collections.singleton(new ShardingSphereRuleConfigurationFixture()), Collections.singleton(new ShardingSphereRuleFixture()));
    
    @Test
    public void assertFindRules() {
        assertThat(ruleMetaData.findRules(ShardingSphereRuleFixture.class).size(), is(1));
    }
    
    @Test
    public void assertFindSingleRule() {
        assertTrue(ruleMetaData.findSingleRule(ShardingSphereRuleFixture.class).isPresent());
    }
    
    @Test
    public void assertFindRuleConfigurations() {
        assertThat(ruleMetaData.findRuleConfigurations(ShardingSphereRuleConfigurationFixture.class).size(), is(1));
    }

    @Test
    public void assertFindSingleRuleConfiguration() {
        assertTrue(ruleMetaData.findSingleRuleConfiguration(ShardingSphereRuleConfigurationFixture.class).isPresent());
    }
}
