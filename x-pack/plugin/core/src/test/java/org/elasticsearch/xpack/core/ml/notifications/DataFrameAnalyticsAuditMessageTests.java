/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.notifications;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.AbstractXContentTestCase;
import org.elasticsearch.xpack.core.common.notifications.Level;

import java.util.Date;

import static org.hamcrest.Matchers.equalTo;

public class DataFrameAnalyticsAuditMessageTests extends AbstractXContentTestCase<DataFrameAnalyticsAuditMessage> {

    public void testGetJobType() {
        DataFrameAnalyticsAuditMessage message = createTestInstance();
        assertThat(message.getJobType(), equalTo("data_frame_analytics"));
    }
    
    @Override
    protected DataFrameAnalyticsAuditMessage doParseInstance(XContentParser parser) {
        return DataFrameAnalyticsAuditMessage.PARSER.apply(parser, null);
    }

    @Override
    protected boolean supportsUnknownFields() {
        return true;
    }

    @Override
    protected DataFrameAnalyticsAuditMessage createTestInstance() {
        return new DataFrameAnalyticsAuditMessage(
            randomBoolean() ? null : randomAlphaOfLength(10),
            randomAlphaOfLengthBetween(1, 20),
            randomFrom(Level.values()),
            new Date(),
            randomBoolean() ? null : randomAlphaOfLengthBetween(1, 20)
        );
    }
}
