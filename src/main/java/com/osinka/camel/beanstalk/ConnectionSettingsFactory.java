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

import com.surftools.BeanstalkClient.Client;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author <a href="mailto:azarov@osinka.com">Alexander Azarov</a>
 */
public class ConnectionSettingsFactory {
    final Pattern HostPortTubeRE = Pattern.compile("^(([\\w.-]+)(:([\\d]+))?/)?([\\w%+]*)$");

    public ConnectionSettings parseUri(final String remaining) throws IllegalArgumentException {
        final Matcher m = HostPortTubeRE.matcher(remaining);
        if (!m.matches())
            throw new IllegalArgumentException(String.format("Invalid path format: %s - should be [<hostName>[:<port>]/][<tubes>]", remaining));

        final String host = m.group(2) != null ? m.group(2) : Client.DEFAULT_HOST;
        final int port = m.group(4) != null ? Integer.parseInt(m.group(4)) : Client.DEFAULT_PORT;
        final String tubes = m.group(5) != null ? m.group(5) : "";
        return new ConnectionSettings(host, port, tubes);
    }
}
