/**
 * Copyright 2014 Transmode AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.transmode.gradle.plugins.docker.client;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class JavaDockerClient implements DockerClient {

    private final DockerClientImpl DOCKER_CLIENT_INSTANCE;

    private static Logger log = Logging.getLogger(JavaDockerClient.class);

    JavaDockerClient() {
        DOCKER_CLIENT_INSTANCE = DockerClientImpl.getInstance();
    }

    JavaDockerClient(String url) {
        DOCKER_CLIENT_INSTANCE = DockerClientImpl.getInstance(url);
    }

    JavaDockerClient(final DockerClientConfig dockerClientConfig) {
        DOCKER_CLIENT_INSTANCE = DockerClientImpl.getInstance(dockerClientConfig);
    }

    @Override
    public String buildImage(File buildDir, String tag) {
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(),  "Image tag can not be empty.");
        final BuildImageCmd buildImageCmd = DOCKER_CLIENT_INSTANCE.buildImageCmd(buildDir).withTag(tag);
        final ResultCallback<BuildResponseItem> buildResponseItemResultCallback = new ResultCallback<BuildResponseItem>() {
            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onNext(BuildResponseItem object) {

            }

            @Override
            public void onError(Throwable throwable) {
                throw new GradleException(
                        "Docker API error: Failed to build Image:\n" + throwable.getMessage());
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void close() throws IOException {

            }
        };
        buildImageCmd.exec(buildResponseItemResultCallback);
        return "build complete";
    }

    @Override
    public String buildImage(File buildDir, List<String> tags) {
        Preconditions.checkNotNull(tags, "Image tags can not be null.");
        Preconditions.checkArgument(!tags.isEmpty(),  "Image tags can not be empty.");
        BuildImageCmd buildImageCmd = DOCKER_CLIENT_INSTANCE.buildImageCmd(buildDir);
        for (String tag : tags) {
            buildImageCmd = buildImageCmd.withTag(tag);
        }
        final ResultCallback<BuildResponseItem> buildResponseItemResultCallback = new ResultCallback<BuildResponseItem>() {
            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onNext(BuildResponseItem object) {

            }

            @Override
            public void onError(Throwable throwable) {
                throw new GradleException(
                        "Docker API error: Failed to build Image:\n" + throwable.getMessage());
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void close() throws IOException {

            }
        };
        buildImageCmd.exec(buildResponseItemResultCallback);
        return "build complete";
    }

    @Override
    public String pushImage(String tag) {
        Preconditions.checkNotNull(tag, "Image tag can not be null.");
        Preconditions.checkArgument(!tag.isEmpty(),  "Image tag can not be empty.");
        final PushImageCmd pushImageCmd = DOCKER_CLIENT_INSTANCE.pushImageCmd(tag);
        final ResultCallback<PushResponseItem> pushResponseItemResultCallback = new ResultCallback<PushResponseItem>() {
            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onNext(PushResponseItem object) {

            }

            @Override
            public void onError(Throwable throwable) {
                throw new GradleException(
                        "Docker API error: Failed to build Image:\n" + throwable.getMessage());
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void close() throws IOException {

            }
        };
        final ResultCallback<PushResponseItem> pushResponseItemResultCallback1 = pushImageCmd.exec(pushResponseItemResultCallback);
        return "Push Complete";
    }

        /*
    private static String checkResponse(ClientResponse response) {
        String msg = response.getEntity(String.class);
        if (response.getStatusInfo() != ClientResponse.Status.OK) {
            throw new GradleException(
                    "Docker API error: Failed to build Image:\n"+msg);
        }
        return msg;
    }
        */

    public static JavaDockerClient create(String url, String user, String password, String email) {
        JavaDockerClient client;
        if (StringUtils.isEmpty(url)) {
            log.info("Connecting to localhost");
            // TODO -- use no-arg constructor once we switch to java-docker 0.9.1
            client = new JavaDockerClient("http://localhost:2375");
        } else {
            log.info("Connecting to {}", url);
            client = new JavaDockerClient(url);
        }

        if (StringUtils.isNotEmpty(user)) {
            DockerClientConfig.DockerClientConfigBuilder dockerClientConfigBuilder = DockerClientConfig.createDefaultConfigBuilder();
            final DockerClientConfig dockerClientConfig = dockerClientConfigBuilder.withUsername(user)
                    .withPassword(password)
                    .withEmail(email)
                    .withUri(url).build();
            client = new JavaDockerClient(dockerClientConfig);
        }

        return client;
    }
}
