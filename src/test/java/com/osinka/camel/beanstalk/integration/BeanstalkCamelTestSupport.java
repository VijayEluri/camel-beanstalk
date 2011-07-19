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

import com.osinka.camel.beanstalk.ConnectionSettings;
import com.osinka.camel.beanstalk.ConnectionSettingsFactory;
import com.surftools.BeanstalkClient.Client;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;

public abstract class BeanstalkCamelTestSupport extends CamelTestSupport {
    final ConnectionSettingsFactory connFactory = ConnectionSettingsFactory.DEFAULT;
    final String tubeName = String.format("test%d", System.currentTimeMillis());

    protected Client reader = null;
    protected Client writer = null;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        ConnectionSettings conn = connFactory.parseUri(tubeName);
        writer = conn.newWritingClient();
        reader = conn.newReadingClient(false);
    }
}
