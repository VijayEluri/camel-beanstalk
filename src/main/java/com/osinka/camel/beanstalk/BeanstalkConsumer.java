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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class BeanstalkConsumer extends ScheduledPollConsumer {
    private final transient Logger log = LoggerFactory.getLogger(getClass());

    String onFailure = BeanstalkComponent.COMMAND_BURY;
    boolean useBlockIO = true;

    private Client client = null;
    private ExecutorService executor = null;
    private Synchronization sync = null;

    private final Runnable initTask = new Runnable() {
            @Override
            public void run() {
                client = getEndpoint().getConnection().newReadingClient(useBlockIO);
            }
        };
    private final Callable<Exchange> pollTask = new Callable<Exchange>() {
        final Integer NO_WAIT = Integer.valueOf(0);
        
        @Override
        public Exchange call() throws Exception {
            if (client == null)
                throw new RuntimeCamelException("Beanstalk client not initialized");

            try {
                final Job job = client.reserve(NO_WAIT);
                if (job == null)
                    return null;

                if (log.isDebugEnabled())
                    log.debug(String.format("Received job ID %d (data length %d)", job.getJobId(), job.getData().length));

                final Exchange exchange = getEndpoint().createExchange(ExchangePattern.InOnly);
                exchange.setProperty(Headers.JOB_ID, job.getJobId());
                exchange.getIn().setBody(job.getData(), byte[].class);
                exchange.addOnCompletion(sync);

                return exchange;
            } catch (BeanstalkException e) {
                log.error("Beanstalk client error", e);
                resetClient();
                return null;
            }
        }

    };

    public BeanstalkConsumer(final BeanstalkEndpoint endpoint, final Processor processor) {
        super(endpoint, processor);
    }

    @Override
    protected int poll() throws Exception {
        int messagesPolled = 0;
        while (isPollAllowed()) {
            final Exchange exchange = executor.submit(pollTask).get();
            if (exchange == null)
                break;

            ++messagesPolled;
            getProcessor().process(exchange);
        }
        return messagesPolled;
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
    protected void doStart() throws Exception {
        executor = getEndpoint().getCamelContext().getExecutorServiceStrategy().newSingleThreadExecutor(this, "Beanstalk");
        executor.execute(initTask);
        sync = new Sync();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        executor.shutdown();
    }

    protected void resetClient() {
        if (client != null)
            client.close();
        initTask.run();
    }

    class Sync implements Synchronization {
        protected final Command successCommand;
        protected final Command failureCommand;

        public Sync() {
            successCommand = new DeleteCommand(getEndpoint());

            if (BeanstalkComponent.COMMAND_BURY.equals(onFailure))
                failureCommand = new BuryCommand(getEndpoint());
            else if (BeanstalkComponent.COMMAND_RELEASE.equals(onFailure))
                failureCommand = new ReleaseCommand(getEndpoint());
            else if (BeanstalkComponent.COMMAND_DELETE.equals(onFailure))
                failureCommand = new DeleteCommand(getEndpoint());
            else
                throw new IllegalArgumentException(String.format("Unknown failure command: %s", onFailure));
        }

        @Override
        public void onComplete(final Exchange exchange) {
            try {
                executor.submit(new RunCommand(successCommand, exchange)).get();
            } catch (Exception e) {
                if (log.isErrorEnabled())
                    log.error(String.format("Could not run completion of exchange %s", exchange), e);
            }
        }

        @Override
        public void onFailure(final Exchange exchange) {
            try {
                executor.submit(new RunCommand(failureCommand, exchange)).get();
            } catch (Exception e) {
                if (log.isErrorEnabled())
                    log.error(String.format("%s could not run failure of exchange %s", failureCommand.getClass().getName(), exchange), e);
            }
        }

        class RunCommand implements Runnable {
            private final Command command;
            private final Exchange exchange;

            public RunCommand(final Command command, final Exchange exchange) {
                this.command = command;
                this.exchange = exchange;
            }

            @Override
            public void run() {
                try {
                    try {
                        command.act(client, exchange);
                    } catch (BeanstalkException e) {
                        if (log.isWarnEnabled())
                            log.warn(String.format("Post-processing %s of exchange %s failed, retrying.", command.getClass().getName(), exchange), e);
                        resetClient();
                        command.act(client, exchange);
                    }
                } catch (final Exception e) {
                    if (log.isErrorEnabled())
                        log.error(String.format("%s could not post-process exchange %s", command.getClass().getName(), exchange), e);
                    exchange.setException(e);
                }
            }
        }
    }
}
