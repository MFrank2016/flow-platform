/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.agent;

import com.flow.platform.agent.config.AgentConfig;
import com.flow.platform.agent.config.UpstreamUrlConfig;
import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.LogListener;
import com.flow.platform.domain.v1.Cmd;
import com.flow.platform.util.CommandUtil.Unix;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.common.base.Charsets;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.glassfish.tyrus.client.ClientManager;

/**
 * Record log to $HOME/agent-log/{cmd id}.out.zip
 * Send log via web socket if real time log enabled and ws url provided
 * <p>
 *
 * @author gy@fir.im
 */
@Log4j2
public class LogEventHandler implements LogListener {

    private final static String REALTIME_LOG_SPLITTER = "#";

    private final Cmd cmd;

    private final Path logPath;

    private Path stdoutLogPath;

    private FileOutputStream stdoutLogStream;

    private ZipOutputStream stdoutLogZipStream;

    private Session wsSession;

    public LogEventHandler(Cmd cmd) {
        AgentConfig config = AgentConfig.getInstance();
        this.cmd = cmd;
        this.logPath = config.getLogDir();

        // init zip log path
        try {
            initZipLogFile(this.cmd);
        } catch (IOException e) {
            log.error("Fail to init cmd log file", e);
        }

        if (!config.getUrl().hasWebsocket()) {
            return;
        }

        try {
            initWebSocketSession(config.getUrl().getWebsocket(), 10);
        } catch (Throwable warn) {
            wsSession = null;
            log.warn("Fail to web socket: " + config.getUrl().getWebsocket() + ": " + warn.getMessage());
        }
    }

    @Override
    public void onLog(Log item) {
        log.debug(item.toString());

        sendRealTimeLog(item);

        // write stdout & stderr
        writeZipStream(stdoutLogZipStream, item.getContent());
    }

    private void sendRealTimeLog(Log item) {
        if (Objects.isNull(wsSession)) {
            return;
        }

        try {
            String format = websocketLogFormat(item);
            wsSession.getBasicRemote().sendText(format);
            log.debug("Log sent: {}", format);
        } catch (Throwable e) {
            log.warn("Fail to send real time log to queue");
        }
    }

    @Override
    public void onFinish() {
        // close socket io
        closeWebSocket();

        if (closeZipAndFileStream(stdoutLogZipStream, stdoutLogStream)) {
            renameAndUpload(stdoutLogPath, Log.Type.STDOUT);
        }
    }

    /**
     * Create websocket log: {cmd.id}#{cmd.type}#{log number}##{log content}
     */
    public String websocketLogFormat(Log log) {
        return new StringBuilder(100)
            .append(cmd.getId()).append(REALTIME_LOG_SPLITTER)
            .append(cmd.getType()).append(REALTIME_LOG_SPLITTER)
            .append(log.getNumber()).append(REALTIME_LOG_SPLITTER)
            .append(log.getContent())
            .toString();
    }

    private void initWebSocketSession(String url, int wsConnectionTimeout) throws Exception {
        CountDownLatch wsLatch = new CountDownLatch(1);
        ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        ClientManager client = ClientManager.createClient();

        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                wsSession = session;
                wsLatch.countDown();
            }
        }, cec, new URI(url));

        if (!wsLatch.await(wsConnectionTimeout, TimeUnit.SECONDS)) {
            throw new TimeoutException("Web socket connection timeout");
        }
    }

    /**
     * Rename xxx.out.tmp to xxx.out.zip and upload zip package to server
     */
    private void renameAndUpload(Path logPath, Log.Type logType) {
        if (!Files.exists(logPath)) {
            return;
        }

        try {
            Path target = Paths.get(logPath.getParent().toString(), getLogFileName(cmd, logType, false));
            Files.move(logPath, target);

            if (logUpload(cmd.getId(), target)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException warn) {
            log.warn("Exception while move update log name from temp: {}", warn.getMessage());
        }
    }

    private boolean logUpload(final String cmdId, final Path path) {
        UpstreamUrlConfig config = AgentConfig.getInstance().getUrl();
        if (!config.hasCmdLog()) {
            return false;
        }

        HttpEntity entity = MultipartEntityBuilder.create()
            .addPart("file", new FileBody(path.toFile(), ContentType.create("application/zip")))
            .addPart("cmdId", new StringBody(cmdId, ContentType.create("text/plain", Charsets.UTF_8)))
            .setContentType(ContentType.MULTIPART_FORM_DATA)
            .build();

        String url = config.getCmdLog();
        HttpResponse<String> response = HttpClient.build(url)
            .post(entity)
            .retry(5)
            .bodyAsString();

        if (!response.hasSuccess()) {
            log.warn("Fail to upload zipped cmd log to: {}", url);
            return false;
        }

        log.trace("Zipped cmd log uploaded on {}", path);
        return true;
    }

    private boolean closeZipAndFileStream(final ZipOutputStream zipStream, final FileOutputStream fileStream) {
        try {
            if (zipStream != null) {
                zipStream.flush();
                zipStream.closeEntry();
                zipStream.close();
                return true;
            }
        } catch (IOException e) {
            log.error("Exception while close zip stream", e);
        } finally {
            try {
                if (fileStream != null) {
                    fileStream.close();
                }
            } catch (IOException e) {
                log.error("Exception while close log file", e);
            }
        }
        return false;
    }

    private void closeWebSocket() {
        if (wsSession != null) {
            try {
                wsSession.close();
            } catch (IOException e) {
                log.warn("Exception while close web socket session");
            }
        }
    }

    private void writeZipStream(final ZipOutputStream stream, final String log) {
        if (stream == null) {
            return;
        }

        // write to zip output stream
        try {
            stream.write(log.getBytes());
            stream.write(Unix.LINE_SEPARATOR.getBytes());
        } catch (IOException e) {
            LogEventHandler.log.warn("Log cannot write: " + log);
        }
    }

    private void initZipLogFile(final Cmd cmd) throws IOException {
        // init log directory
        try {
            Files.createDirectories(logPath);
        } catch (FileAlreadyExistsException ignore) {
            log.warn("Log path {} already exist", logPath);
        }

        // init zipped log file for tmp
        Path stdoutPath = Paths.get(logPath.toString(), getLogFileName(cmd, Log.Type.STDOUT, true));
        Files.deleteIfExists(stdoutPath);

        stdoutLogPath = Files.createFile(stdoutPath);

        // init zip stream for stdout log
        stdoutLogStream = new FileOutputStream(stdoutLogPath.toFile());
        stdoutLogZipStream = new ZipOutputStream(stdoutLogStream);
        ZipEntry outEntry = new ZipEntry(cmd.getId() + ".out");
        stdoutLogZipStream.putNextEntry(outEntry);
    }

    private String getLogFileName(Cmd cmd, Log.Type logType, boolean isTemp) {
        String logTypeSuffix = logType == Log.Type.STDERR ? ".err" : ".out";
        String tempSuffix = isTemp ? ".tmp" : ".zip";

        // replace / with - since cmd id may includes slash which the same as dir path
        return cmd.getId().replace(Unix.PATH_SEPARATOR.charAt(0), '-') + logTypeSuffix + tempSuffix;
    }
}
