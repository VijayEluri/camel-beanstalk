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

import com.osinka.camel.beanstalk.processors.*;
import com.surftools.BeanstalkClient.BeanstalkException;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClient.Job;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.PollingConsumerSupport;

/**
 * PollingConsumer to read Beanstalk jobs.
 *
 * This consumer will add a {@link Synchronization} object to every {@link Exchange}
 * object it creates in order to react on successful exchange completion or failure.
 *
 * In the case of successful completion, Beanstalk's <code>delete</code> method is
 * called upon the job. In the case of failure the default reaction is to call
 * <code>bury</code>.
 *
 * The only configuration this consumer may have is the reaction on failures: possible
 * variants are "bury", "release" or "delete"
 *
 * @author <a href="mailto:azarov@osinka.com">Alexander Azarov</a>
 */
public class BeanstalkConsumer extends PollingConsumerSupport {
    private final transient Log LOG = LogFactory.getLog(BeanstalkConsumer.class);

    final Synchronization sync = new ExchangeSync();
    Client client = null;
    String onFailure = BeanstalkComponent.COMMAND_BURY;
    boolean useBlockIO = true;

    public BeanstalkConsumer(final BeanstalkEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public Exchange receiveNoWait() {
        return reserve(Integer.valueOf(0));
    }

    @Override
    public Exchange receive() {
        return reserve(null);
    }

    @Override
    public Exchange receive(final long timeout) {
        return reserve( Integer.valueOf((int)timeout) );
    }

    private void initClient() {
        client = getEndpoint().getConnection().newReadingClient(useBlockIO);
    }

    private void closeClient() {
        if (client != null)
            client.close();
    }

    Exchange reserve(final Integer timeout) {
        if (client == null)
            throw new RuntimeCamelException("Beanstalk client not initialized");

        try {
            final Job job = client.reserve(timeout);
            if (job == null)
                return null;

            if (LOG.isDebugEnabled())
                LOG.debug(String.format("Received job ID %d (data length %d)", job.getJobId(), job.getData().length));

            final Exchange exchange = getEndpoint().createExchange(ExchangePattern.InOnly);
            exchange.setProperty(Headers.JOB_ID, job.getJobId());
            exchange.getIn().setBody(job.getData(), byte[].class);
            exchange.addOnCompletion(sync);

            return exchange;
        } catch (BeanstalkException e) {
            LOG.error("Beanstalk client error", e);
            closeClient();
            initClient();
            return null;
        }
    }

    public String getOnFailure() {
        return onFailure;
    }

    public void setOnFailure(String onFailure) {
        this.onFailure = onFailure;
    }

    public boolean getUseBlockIO() {
        return useBlockIO;
    }

    public void setUseBlockIO(boolean useBlockIO) {
        this.useBlockIO = useBlockIO;
    }

    @Override
    public BeanstalkEndpoint getEndpoint() {
        return (BeanstalkEndpoint) super.getEndpoint();
    }

    @Override
    protected void doStart() {
        initClient();
    }

    @Override
    protected void doStop() {
        closeClient();
    }

    class ExchangeSync implements Synchronization {
        @Override
        public void onComplete(final Exchange exchange) {
            final Processor processor = new DeleteProcessor(getEndpoint(), client);
            try {
                processor.process(exchange);
            } catch (final Exception e) {
                if (LOG.isFatalEnabled())
                    LOG.fatal(String.format("%s failed to onComplete %s", getEndpoint().getConnection(), exchange), e);
                exchange.setException(e);
            }
        }

        @Override
        public void onFailure(final Exchange exchange) {
            Processor processor = null;
            if (BeanstalkComponent.COMMAND_BURY.equals(onFailure))
                processor = new BuryProcessor(getEndpoint(), client);
            else if (BeanstalkComponent.COMMAND_RELEASE.equals(onFailure))
                processor = new ReleaseProcessor(getEndpoint(), client);
            else if (BeanstalkComponent.COMMAND_DELETE.equals(onFailure))
                processor = new DeleteProcessor(getEndpoint(), client);
            else
                return;

            try {
                processor.process(exchange);
            } catch (final Exception e) {
                if (LOG.isFatalEnabled())
                    LOG.fatal(String.format("%s failed to onComplete %s", getEndpoint().getConnection(), exchange), e);
                exchange.setException(e);
            }
        }
    }
}
