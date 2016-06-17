/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package herddb.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Server configuration
 *
 * @author enrico.olivelli
 */
public final class ServerConfiguration {

    private final Properties properties;

    public static final String PROPERTY_NODEID = "server.nodeId";
    public static final String PROPERTY_MODE = "server.mode";

    /**
     * Accept requests only for TableSpaces for which the local server is leader
     */
    public static final String PROPERTY_ENFORCE_LEADERSHIP = "server.enforceleadership";
    public static final boolean PROPERTY_ENFORCE_LEADERSHIP_DEFAULT = true;

    public static final String PROPERTY_MODE_LOCAL = "local";
    public static final String PROPERTY_MODE_STANDALONE = "standalone";
    public static final String PROPERTY_MODE_CLUSTER = "cluster";

    public static final String PROPERTY_BASEDIR = "server.baseDir";
    public static final String PROPERTY_HOST = "server.host";
    public static final String PROPERTY_HOST_DEFAULT = "localhost";
    public static final String PROPERTY_PORT = "server.port";
    public static final String PROPERTY_SSL = "server.ssl";

    public static final String PROPERTY_ZOOKEEPER_ADDRESS = "server.zookeeper.address";
    public static final String PROPERTY_ZOOKEEPER_SESSIONTIMEOUT = "server.zookeeper.sessiontimeout";
    public static final String PROPERTY_ZOOKEEPER_PATH = "server.zookeeper.path";

    public static final String PROPERTY_BOOKKEEPER_START = "server.bookkeeper.start";
    public static final boolean PROPERTY_BOOKKEEPER_START_DEFAULT = false;

    public static final String PROPERTY_BOOKKEEPER_BOOKIE_PORT = "bookie.port";
    public static final int PROPERTY_BOOKKEEPER_BOOKIE_PORT_DEFAULT = 3181;

    public static final String PROPERTY_BOOKKEEPER_ENSEMBLE = "server.bookkeeper.ensemble";
    public static final int PROPERTY_BOOKKEEPER_ENSEMBLE_DEFAULT = 1;
    public static final String PROPERTY_BOOKKEEPER_WRITEQUORUMSIZE = "server.bookkeeper.writequorumsize";
    public static final int PROPERTY_BOOKKEEPER_WRITEQUORUMSIZE_DEFAULT = 1;
    public static final String PROPERTY_BOOKKEEPER_ACKQUORUMSIZE = "server.bookkeeper.ackquorumsize";
    public static final int PROPERTY_BOOKKEEPER_ACKQUORUMSIZE_DEFAULT = 1;
    public static final String PROPERTY_BOOKKEEPER_LOGRETENTION_PERIOD = "server.bookkeeper.logretentionperiod";

    public static final String PROPERTY_LOG_RETENTION_PERIOD = "server.log.retention.period";
    public static final long PROPERTY_LOG_RETENTION_PERIOD_DEFAULT = 1000L * 10 * 60 * 24 * 2;

    public static final String PROPERTY_ZOOKEEPER_ADDRESS_DEFAULT = "localhost:1281";
    public static final String PROPERTY_ZOOKEEPER_PATH_DEFAULT = "/herd";
    public static final int PROPERTY_PORT_DEFAULT = 7000;
    public static final int PROPERTY_ZOOKEEPER_SESSIONTIMEOUT_DEFAULT = 40000;

    public static final String PROPERTY_USERS_FILE = "server.users.file";
    public static final String PROPERTY_USERS_FILE_DEFAULT = "";

    public static final String PROPERTY_SERVER_TO_SERVER_USERNAME = "server.username";
    public static final String PROPERTY_SERVER_TO_SERVER_PASSWORD = "server.password";

    public static final String PROPERTY_DISK_SWAP_MAX_RECORDS = "server.disk.swap.max.records";
    public static final int PROPERTY_DISK_SWAP_MAX_RECORDS_DEFAULT = 5000;

    public ServerConfiguration(Properties properties) {
        this.properties = new Properties();
        this.properties.putAll(properties);
    }

    public ServerConfiguration(Path baseDir) {
        this();
        set(PROPERTY_BASEDIR, baseDir.toAbsolutePath());
    }

    /**
     * Copy configuration
     *
     * @return
     */
    public ServerConfiguration copy() {
        Properties copy = new Properties();
        copy.putAll(this.properties);
        return new ServerConfiguration(copy);
    }

    public ServerConfiguration() {
        this.properties = new Properties();
    }

    public int getInt(String key, int defaultValue) {
        final String value = this.properties.getProperty(key);

        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        final String value = this.properties.getProperty(key);

        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    public long getLong(String key, long defaultValue) {
        final String value = this.properties.getProperty(key);

        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        return Long.parseLong(value);
    }

    public String getString(String key, String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }

    public ServerConfiguration set(String key, Object value) {
        if (value == null) {
            this.properties.remove(key);
        } else {
            this.properties.setProperty(key, value + "");
        }
        return this;
    }

    @Override
    public String toString() {
        return "ServerConfiguration{" + "properties=" + properties + '}';
    }

    public void readJdbcUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        int questionMark = url.indexOf('?');
        if (questionMark <= 0) {
            questionMark = url.length();
        }
        String before = url.substring(0, questionMark);
        if (!before.startsWith("jdbc:herddb:")) {
            throw new IllegalArgumentException("invalid url " + url);
        }
        if (before.startsWith("jdbc:herddb:zookeeper:")) {
            set(PROPERTY_MODE, PROPERTY_MODE_CLUSTER);
            String zkaddress = before.substring("jdbc:herddb:zookeeper:".length());
            int slash = zkaddress.indexOf('/');
            if (slash <= 0) {
                set(PROPERTY_ZOOKEEPER_ADDRESS, zkaddress);
            } else {
                String path = zkaddress.substring(slash);
                zkaddress = zkaddress.substring(0, slash);
                set(PROPERTY_ZOOKEEPER_ADDRESS, zkaddress);
                set(PROPERTY_ZOOKEEPER_PATH, path);
            }
        } else if (before.startsWith("jdbc:herddb:server:")) {
            set(PROPERTY_MODE, PROPERTY_MODE_STANDALONE);
            before = before.substring("jdbc:herddb:server:".length());
            int port_pos = before.indexOf(':');
            String host = before;
            int port = PROPERTY_PORT_DEFAULT;
            if (port_pos > 0) {
                host = before.substring(0, port_pos);
                port = Integer.parseInt(before.substring(port_pos + 1));
            }
            set(PROPERTY_HOST, host);
            set(PROPERTY_PORT, port);
        } else if (before.startsWith("jdbc:herddb:local:")) {
            set(PROPERTY_MODE, PROPERTY_MODE_LOCAL);
        }
        String qs = url.substring(questionMark + 1);
        String[] params = qs.split("&");
        LOG.log(Level.SEVERE, "url " + url + " qs " + qs + " params " + Arrays.toString(params));
        for (String param : params) {
            // TODO: URLDecoder??
            int pos = param.indexOf('=');
            if (pos > 0) {
                String key = param.substring(0, pos);
                String value = param.substring(pos + 1);
                set(key, value);
            } else {
                set(param, "");
            }
        }
    }
    private static final Logger LOG = Logger.getLogger(ServerConfiguration.class.getName());

    public List<String> keys() {
        return properties.keySet().stream().map(Object::toString).sorted().collect(Collectors.toList());
    }

}
