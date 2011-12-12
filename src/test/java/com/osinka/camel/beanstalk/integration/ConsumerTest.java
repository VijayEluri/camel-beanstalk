/**
 * Copyright (C) 2010 Osinka <http://osinka.ru>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osinka.camel.beanstalk.integration;

import com.osinka.camel.beanstalk.Headers;
import com.osinka.camel.beanstalk.Helper;
import java.io.IOException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;

public class ConsumerTest extends BeanstalkCamelTestSupport {
    final String testMessage = "Hello, world!";

    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    @Test
    public void testReceive() throws IOException, InterruptedException {
        long PRIO = 0;
        int TTR = 10;
        final long jobId = writer.put(PRIO, 0, TTR, Helper.stringToBytes(testMessage));

        result.expectedMessageCount(1);
        result.expectedPropertyReceived(Headers.JOB_ID, jobId);
        result.message(0).header(Exchange.CREATED_TIMESTAMP).isNotNull();
        result.message(0).header(Headers.JOB_ID).isEqualTo(Long.valueOf(jobId));
        result.message(0).header(Headers.PRIORITY).isEqualTo(Long.valueOf(PRIO));
        result.message(0).header(Headers.TUBE).isEqualTo(tubeName);
        result.message(0).header(Headers.STATE).isEqualTo("reserved");
        result.message(0).header(Headers.AGE).isGreaterThan(0);
        result.message(0).header(Headers.TIME_LEFT).isGreaterThan(0);
        result.message(0).header(Headers.TIMEOUTS).isNotNull();
        result.message(0).header(Headers.RELEASES).isNotNull();
        result.message(0).header(Headers.BURIES).isNotNull();
        result.message(0).header(Headers.KICKS).isNotNull();
        result.message(0).body().isEqualTo(testMessage);
        result.assertIsSatisfied(500);
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("beanstalk:"+tubeName).to("mock:result");
            }
        };
    }
}
