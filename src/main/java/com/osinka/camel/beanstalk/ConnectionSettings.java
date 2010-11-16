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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Scanner;
import java.net.URLDecoder;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClientImpl.ClientImpl;
import java.io.UnsupportedEncodingException;

/**
 * Represents the connection to Beanstalk.
 * <p>
 * Along with the list of tubes it may watch.
 *
 * @author <a href="mailto:azarov@osinka.com">Alexander Azarov</a>
 */
public class ConnectionSettings {
    final String host;
    final int port;
    final String[] tubes;

    public ConnectionSettings(final String tube) {
        this(Client.DEFAULT_HOST, Client.DEFAULT_PORT, tube);
    }

    public ConnectionSettings(final String host, final String tube) {
        this(host, Client.DEFAULT_PORT, tube);
    }

    public ConnectionSettings(final String host, final int port, final String tube) {
        this.host = host;
        this.port = port;

        final Scanner scanner = new Scanner(tube);
        scanner.useDelimiter("\\+");
        final ArrayList<String> buffer = new ArrayList<String>();
        while (scanner.hasNext()) {
            final String tubeRaw = scanner.next();
            try {
                buffer.add( URLDecoder.decode(tubeRaw, "UTF-8") );
            } catch (UnsupportedEncodingException e) {
                buffer.add(tubeRaw);
            }
        }
        this.tubes = buffer.toArray(new String[0]);
        scanner.close();
    }

    /**
     * Returns the {@link Client} instance ready for writing
     * operations, e.g. "put".
     * <p>
     * <code>use(tube)</code> is applied during this call.
     *
     * @return {@link Client} instance
     * @throws IllegalArgumentException the exception is raised when this ConnectionSettings
     * has more than one tube.
     */
    public Client newWritingClient() throws IllegalArgumentException {
        if (tubes.length > 1) {
            throw new IllegalArgumentException("There must be only one tube specified for Beanstalk producer");
        }

        final String tube = tubes.length > 0 ? tubes[0] : BeanstalkComponent.DEFAULT_TUBE;

        final ClientImpl client = new ClientImpl(host, port);
        client.setUniqueConnectionPerThread(false);
        client.useTube(tube);
        return client;
    }

    /**
     * Returns the {@link Client} instance for reading operations with all
     * the tubes aleady watched
     * <p>
     * <code>watch(tube)</code> is applied for every tube during this call.
     *
     * @param useBlockIO configuration param to {@link Client}
     * @return {@link Client} instance
     */
    public Client newReadingClient(boolean useBlockIO) {
        final ClientImpl client = new ClientImpl(host, port, useBlockIO);
        client.setUniqueConnectionPerThread(false);
        for (String tube : tubes)
            client.watch(tube);
        return client;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ConnectionSettings) {
            final ConnectionSettings other = (ConnectionSettings) obj;
            return other.host.equals(host) && other.port == port && Arrays.equals(other.tubes, tubes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 41*(41*(41+host.hashCode())+port)+Arrays.hashCode(tubes);
    }

    @Override
    public String toString() {
        return "beanstalk://"+host+":"+port+"/"+Arrays.toString(tubes);
    }
}