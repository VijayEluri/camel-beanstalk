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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future; 
import com.osinka.camel.beanstalk.processors.Command;
import com.surftools.BeanstalkClient.BeanstalkException;
import com.surftools.BeanstalkClient.Client;
import org.apache.camel.Exchange;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.AsyncCallback;
import org.apache.camel.impl.DefaultProducer;

/**
 *
 * @author <a href="mailto:azarov@osinka.com">Alexander Azarov</a>
 */
public class BeanstalkProducer extends DefaultProducer implements AsyncProcessor {
    private ExecutorService executor = null;

    Client client = null;
    final Command command;

    public BeanstalkProducer(BeanstalkEndpoint endpoint, final Command command) throws Exception {
        super(endpoint);
        this.command = command;
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        Future f = executor.submit(new RunCommand(exchange));
        f.get();
    }

    @Override
    public boolean process(final Exchange exchange, final AsyncCallback callback) {
        try {
            executor.submit(new RunCommand(exchange, callback));
        } catch (Throwable t) {
            exchange.setException(t);
            callback.done(true);
            return true;
        }
        return false;
    }

    protected void resetClient() {
        closeClient();
        initClient();
    }

    protected void closeClient() {
        if (client != null)
            client.close();
    }

    protected void initClient() {
        this.client = getEndpoint().getConnection().newWritingClient();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        executor = getEndpoint().getCamelContext().getExecutorServiceStrategy().newSingleThreadExecutor(this, "Beanstalk");
        executor.execute(new Runnable() {
                public void run() {
                    initClient();
                }
            });
    }

    @Override
    protected void doStop() throws Exception {
        executor.shutdown();
        closeClient();
        super.doStop();
    }

    @Override
    public BeanstalkEndpoint getEndpoint() {
        return (BeanstalkEndpoint) super.getEndpoint();
    }

    class RunCommand implements Runnable {
        private final Exchange exchange;
        private final AsyncCallback callback;

        public RunCommand(final Exchange exchange) {
            this(exchange, null);
        }

        public RunCommand(final Exchange exchange, final AsyncCallback callback) {
            this.exchange = exchange;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                try {
                    command.act(client, exchange);
                } catch (BeanstalkException e) {
                    /* Retry one time */
                    resetClient();
                    command.act(client, exchange);
                }
            } catch (Throwable t) {
                exchange.setException(t);
            } finally {
                if (callback != null)
                    callback.done(false);
            }
        }
    }
}
