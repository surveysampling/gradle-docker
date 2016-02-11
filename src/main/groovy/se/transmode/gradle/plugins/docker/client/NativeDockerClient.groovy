/*
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
package se.transmode.gradle.plugins.docker.client

import com.google.common.base.Preconditions
import org.apache.commons.io.IOUtils
import org.gradle.api.GradleException

import java.nio.charset.StandardCharsets

class NativeDockerClient implements DockerClient {

    private final String binary;

    private final String pushArgs;

    NativeDockerClient(String binary) {
        Preconditions.checkArgument(binary as Boolean,  "Docker binary can not be empty or null.")
        this.binary = binary
        // For Docker Versions < 1.10.0 when pushing to a private repository on Docker Hub use -f. FIXME
        this.pushArgs = isDockerVersionLessThanOnePointTen() ? "-f" : ""
    }

    @Override
    String buildImage(File buildDir, String tag) {
        Preconditions.checkArgument(tag as Boolean,  "Image tag can not be empty or null.")
        final cmdLine = "${binary} build -t ${tag} ${buildDir}"
        return executeAndWait(cmdLine)
    }

    @Override
    String pushImage(String tag) {
        Preconditions.checkArgument(tag as Boolean,  "Image tag can not be empty or null.")
        final cmdLine = "${binary} push ${pushArgs} ${tag}"
        return executeAndWait(cmdLine)
    }

    private static String executeAndWait(String cmdLine) {
        println cmdLine
        final TIMEOUT_IN_MILLIS = 120000
        def process = cmdLine.execute()
        process.consumeProcessOutput(System.out, System.err)
        println 'Requested to consume process output:'
        process.waitForOrKill(TIMEOUT_IN_MILLIS)
        if (process.exitValue()) {
            throw new GradleException("Docker execution failed\nCommand line [${cmdLine}] returned:\n${process.err.text}")
        }
        return process.in.text
    }

    // FIXME This is a hack.
    private static boolean isDockerVersionLessThanOnePointTen() {
        final cmdLine = "docker -v"
        final TIMEOUT_IN_MILLIS = 10000
        def process = cmdLine.execute()
        process.waitForOrKill(TIMEOUT_IN_MILLIS)
        if (process.exitValue()) {
            throw new GradleException("Docker execution failed\nCommand line [${cmdLine}] returned:\n${process.err.text}")
        }
        def processInputStreamText = IOUtils.toString(process.in, StandardCharsets.UTF_8);
        println processInputStreamText
        final listOfStrings = processInputStreamText.tokenize(', ').get(2).tokenize('.')
        final isLessThanOnePointTen = Integer.parseInt(listOfStrings[0]) <= 1 && Integer.parseInt(listOfStrings[1]) < 10
        return isLessThanOnePointTen
    }

}
