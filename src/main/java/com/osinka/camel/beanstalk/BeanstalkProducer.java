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

import com.osinka.camel.beanstalk.processors.Command;
import com.surftools.BeanstalkClient.BeanstalkException;
import com.surftools.BeanstalkClient.Client;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

/**
 *
 * @author <a href="mailto:azarov@osinka.com">Alexander Azarov</a>
 */
public class BeanstalkProducer extends DefaultProducer {
    Client client = null;
    final Command command;

    public BeanstalkProducer(BeanstalkEndpoint endpoint, final Command command) throws Exception {
        super(endpoint);
        this.command = command;
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        try {
            command.act(client, exchange);
        } catch (BeanstalkException e) {
            stop();
            start();
            command.act(client, exchange);
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        this.client = getEndpoint().getConnection().newWritingClient();
    }

    @Override
    protected void doStop() throws Exception {
        if (client != null)
            client.close();
        super.doStop();
    }

    @Override
    public BeanstalkEndpoint getEndpoint() {
        return (BeanstalkEndpoint) super.getEndpoint();
    }
}
