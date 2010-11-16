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

import java.util.Map;
import java.util.HashMap;
import com.surftools.BeanstalkClient.Client;
import org.apache.camel.Component;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.ResolveEndpointFailedException;
import com.osinka.camel.beanstalk.processors.*;
import org.apache.camel.impl.DefaultPollingEndpoint;
import org.apache.camel.util.EndpointHelper;

/**
 * @author <a href="mailto:azarov@osinka.com">Alexander Azarov</a>
 * @see BeanstalkConsumer
 * @see PutProducer
 */
public class BeanstalkEndpoint extends DefaultPollingEndpoint {
    final ConnectionSettings conn;

    String command      = BeanstalkComponent.COMMAND_PUT;
    long priority       = BeanstalkComponent.DEFAULT_PRIORITY;
    int delay           = BeanstalkComponent.DEFAULT_DELAY;
    int timeToRun       = BeanstalkComponent.DEFAULT_TIME_TO_RUN;

    private Map<String, Object> pollingConsumerProperties = new HashMap<String,Object>();

    BeanstalkEndpoint(final String uri, final Component component, final ConnectionSettings conn) {
        super(uri, component);

        this.conn = conn;
    }

    public ConnectionSettings getConnection() {
        return conn;
    }

    /**
     * The command {@link Producer} must execute
     *
     * @param command
     */
    public void setCommand(final String command) {
        this.command = command;
    }

    public void setJobPriority(final long priority) {
        this.priority = priority;
    }

    public long getJobPriority() {
        return priority;
    }

    public void setJobDelay(final int delay) {
        this.delay = delay;
    }

    public int getJobDelay() {
        return delay;
    }

    public void setJobTimeToRun(final int timeToRun) {
        this.timeToRun = timeToRun;
    }

    public int getJobTimeToRun() {
        return timeToRun;
    }

    /**
     * Creates Camel producer.
     * <p>
     * Depending on the command parameter (see {@link BeanstalkComponent} URI) it
     * will create one of the producer implementations.
     *
     * @return {@link Producer} instance
     * @throws IllegalArgumentException when {@link ConnectionSettings} cannot
     * create a writable {@link Client}
     */
    @Override
    public Producer createProducer() throws Exception {
        CommandProcessor processor = null;
        if (BeanstalkComponent.COMMAND_PUT.equals(command))
            processor = new PutProcessor(this);
        else if (BeanstalkComponent.COMMAND_RELEASE.equals(command))
            processor = new ReleaseProcessor(this);
        else if (BeanstalkComponent.COMMAND_BURY.equals(command))
            processor = new BuryProcessor(this);
        else if (BeanstalkComponent.COMMAND_TOUCH.equals(command))
            processor = new TouchProcessor(this);
        else if (BeanstalkComponent.COMMAND_DELETE.equals(command))
            processor = new DeleteProcessor(this);
        else if (BeanstalkComponent.COMMAND_KICK.equals(command))
            processor = new KickProcessor(this);
        else
            throw new IllegalArgumentException(String.format("Unknown command for Beanstalk endpoint: %s", command));

        return new BeanstalkProducer(this, processor);
    }

    @Override
    public PollingConsumer createPollingConsumer() throws Exception {
        BeanstalkConsumer consumer = new BeanstalkConsumer(this);
        configurePollingConsumer(consumer);
        return consumer;
    }

    @Override 
    public void setConsumerProperties(Map<String, Object> consumerProperties) {
        super.setConsumerProperties(consumerProperties);
        setPollingConsumerProperties(consumerProperties);
    }

    public void setPollingConsumerProperties(Map<String, Object> consumerProperties) {
        pollingConsumerProperties.putAll(consumerProperties);
        consumerProperties.clear();
    }

    protected void configurePollingConsumer(BeanstalkConsumer consumer) throws Exception {
        if (!pollingConsumerProperties.isEmpty()) {
            EndpointHelper.setProperties(getCamelContext(), consumer, pollingConsumerProperties);
            if (pollingConsumerProperties.size() > 0) {
                throw new ResolveEndpointFailedException(this.getEndpointUri(), "There are " + pollingConsumerProperties.size()
                    + " parameters that couldn't be set on the endpoint consumer."
                    + " Check the uri if the parameters are spelt correctly and that they are properties of the endpoint."
                    + " Unknown consumer parameters=[" + pollingConsumerProperties + "]");
            }
        }
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
}
