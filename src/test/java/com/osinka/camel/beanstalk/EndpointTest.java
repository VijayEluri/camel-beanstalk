/**
 * Copyright (C) 2010 Alexander Azarov <azarov@osinka.com>
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

package com.osinka.camel.beanstalk;

import org.apache.camel.CamelContext;
import org.apache.camel.FailedToCreateProducerException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class EndpointTest {
    CamelContext context = null;

    @Before
    public void setUp() throws Exception {
        context = new DefaultCamelContext();
        context.disableJMX();
        context.start();
    }

    @Test
    public void testPriority() {
        BeanstalkEndpoint endpoint = context.getEndpoint("beanstalk:default?jobPriority=1000", BeanstalkEndpoint.class);
        assertNotNull("Beanstalk endpoint", endpoint);
        assertEquals("Priority", 1000, endpoint.getJobPriority());
    }

    @Test
    public void testTimeToRun() {
        BeanstalkEndpoint endpoint = context.getEndpoint("beanstalk:default?jobTimeToRun=10", BeanstalkEndpoint.class);
        assertNotNull("Beanstalk endpoint", endpoint);
        assertEquals("Time to run", 10, endpoint.getJobTimeToRun());
    }

    @Test
    public void testDelay() {
        BeanstalkEndpoint endpoint = context.getEndpoint("beanstalk:default?jobDelay=10", BeanstalkEndpoint.class);
        assertNotNull("Beanstalk endpoint", endpoint);
        assertEquals("Delay", 10, endpoint.getJobDelay());
    }

    @Test
    public void testCommand() {
        BeanstalkEndpoint endpoint = context.getEndpoint("beanstalk:default?command=release", BeanstalkEndpoint.class);
        assertNotNull("Beanstalk endpoint", endpoint);
        assertEquals("Command", BeanstalkComponent.COMMAND_RELEASE, endpoint.command);
    }

    @Test
    public void testTubes() {
        BeanstalkEndpoint endpoint = context.getEndpoint("beanstalk:host:11303/tube1+tube%2B+tube%3F?command=kick", BeanstalkEndpoint.class);
        assertNotNull("Beanstalk endpoint", endpoint);
        assertEquals("Command", BeanstalkComponent.COMMAND_KICK, endpoint.command);
        assertArrayEquals("Tubes", new String[] {"tube1", "tube+", "tube?"}, endpoint.conn.tubes);
    }

    @Test(expected=FailedToCreateProducerException.class)
    public void testWrongCommand() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").to("beanstalk:default?command=noCommand");
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        context.stop();
    }
}
