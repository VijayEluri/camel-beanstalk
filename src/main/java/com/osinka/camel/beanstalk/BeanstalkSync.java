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
import org.apache.camel.Exchange;
import org.apache.camel.impl.ServiceSupport;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BeanstalkSync extends ServiceSupport implements Synchronization {
    private final transient Log LOG = LogFactory.getLog(BeanstalkSync.class);

    protected Client client = null;
    protected final BeanstalkEndpoint endpoint;
    protected final Command successCommand;
    protected final Command failureCommand;

    public BeanstalkSync(final BeanstalkEndpoint endpoint, final String onFailure) {
        this.endpoint = endpoint;
        successCommand = new DeleteCommand(endpoint);

        if (BeanstalkComponent.COMMAND_BURY.equals(onFailure))
            failureCommand = new BuryCommand(endpoint);
        else if (BeanstalkComponent.COMMAND_RELEASE.equals(onFailure))
            failureCommand = new ReleaseCommand(endpoint);
        else if (BeanstalkComponent.COMMAND_DELETE.equals(onFailure))
            failureCommand = new DeleteCommand(endpoint);
        else
            throw new IllegalArgumentException(String.format("Unknown failure command: %s", onFailure));
    }

    @Override
    public void onComplete(final Exchange exchange) {
        try {
            try {
                successCommand.act(client, exchange);
            } catch (BeanstalkException e) {
                stop();
                start();
                successCommand.act(client, exchange);
            }
        } catch (final Exception e) {
            if (LOG.isFatalEnabled())
                LOG.fatal(String.format("%s could not delete job %d of exchange %s", endpoint.getConnection(), exchange.getProperty(Headers.JOB_ID), exchange), e);
            exchange.setException(e);
        }
    }

    @Override
    public void onFailure(final Exchange exchange) {
        try {
            try {
                failureCommand.act(client, exchange);
            } catch (BeanstalkException e) {
                stop();
                start();
                failureCommand.act(client, exchange);
            }
        } catch (final Exception e) {
            if (LOG.isFatalEnabled())
                LOG.fatal(String.format("%s could not post-process job %d of exchange %s", endpoint.getConnection(), exchange.getProperty(Headers.JOB_ID), exchange), e);
            exchange.setException(e);
        }
    }

    @Override
    protected void doStart() {
        client = endpoint.getConnection().newWritingClient();
    }

    @Override
    protected void doStop() {
        if (client != null)
            client.close();
    }
}