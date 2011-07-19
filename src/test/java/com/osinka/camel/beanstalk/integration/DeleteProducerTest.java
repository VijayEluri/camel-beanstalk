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
import com.surftools.BeanstalkClient.Job;
import java.io.IOException;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class DeleteProducerTest extends BeanstalkCamelTestSupport {
    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate direct;

    @Test
    public void testDelete() throws InterruptedException, IOException {
        long jobId = writer.put(0, 0, 5, new byte[0]);
        assertTrue("Valid Job Id", jobId > 0);

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.allMessages().header(Headers.JOB_ID).isNotNull();
        resultEndpoint.allMessages().header(Headers.RESULT).isEqualTo(true);
        direct.sendBodyAndHeader(null, Headers.JOB_ID, jobId);

        assertMockEndpointsSatisfied();

        final Long messageJobId = resultEndpoint.getReceivedExchanges().get(0).getIn().getHeader(Headers.JOB_ID, Long.class);
        assertNotNull("Job ID in message", messageJobId);
        assertEquals("Message Job ID equals", jobId, messageJobId.longValue());

        final Job job = reader.peek(jobId);
        assertNull("Job has been deleted", job);
    }

    @Test(expected=CamelExecutionException.class)
    public void testNoJobId() throws InterruptedException, IOException {
        resultEndpoint.expectedMessageCount(0);
        direct.sendBody(new byte[0]);

        resultEndpoint.assertIsSatisfied();
        assertListSize("Number of exceptions", resultEndpoint.getFailures(), 1);
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").to("beanstalk:"+tubeName+"?command=delete").to("mock:result");
            }
        };
    }
}
