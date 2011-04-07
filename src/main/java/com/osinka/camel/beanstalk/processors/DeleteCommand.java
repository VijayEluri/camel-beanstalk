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

package com.osinka.camel.beanstalk.processors;

import com.osinka.camel.beanstalk.BeanstalkEndpoint;
import com.osinka.camel.beanstalk.BeanstalkExchangeHelper;
import com.osinka.camel.beanstalk.Headers;
import com.surftools.BeanstalkClient.Client;
import org.apache.camel.Exchange;
import org.apache.camel.NoSuchHeaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteCommand extends DefaultCommand {
    private final transient Logger log = LoggerFactory.getLogger(getClass());

    public DeleteCommand(BeanstalkEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void act(final Client client, final Exchange exchange) throws NoSuchHeaderException {
        final Long jobId = BeanstalkExchangeHelper.getJobID(exchange);
        final boolean result = client.delete(jobId.longValue());
        if (!result && log.isWarnEnabled())
            log.warn(String.format("Failed to delete job %d", jobId));
        else if (log.isDebugEnabled())
            log.debug(String.format("Job %d deleted. Result is %b", jobId, result));

        answerWith(exchange, Headers.RESULT, result);
    }
}
