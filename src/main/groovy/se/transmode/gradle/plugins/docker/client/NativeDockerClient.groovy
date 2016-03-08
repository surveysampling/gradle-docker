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
import org.gradle.api.GradleException

class NativeDockerClient implements DockerClient {

    private final String binary

    private final String pushArgs

    private final boolean isCentOSDockerBinary

    NativeDockerClient(String binary) {
        Preconditions.checkArgument(binary as Boolean,  "Docker binary can not be empty or null.")
        this.binary = binary
        this.isCentOSDockerBinary = isCentOSDocker()
        this.pushArgs = isCentOSDockerBinary ? "-f" : ""
    }

    private static Boolean isCentOSDocker() {
        final cmdLine = ["docker", "-v"]
        final TIMEOUT_IN_MILLIS = 10000
        final process = cmdLine.execute()
        process.waitForOrKill(TIMEOUT_IN_MILLIS)
        final exitValue = process.exitValue()
        if (exitValue) { // http://groovy-lang.org/semantics.html#_numbers
            throw new GradleException("Docker execution failed\nCommand line [${cmdLine}] returned:\n${process.err.text}")
        }
        return process.text.contains("centos")
    }

    @Override
    String buildImage(File buildDir, String tag) {
        Preconditions.checkArgument(tag as Boolean,  "Image tag can not be empty or null.")
        def cmdLine = [isCentOSDockerBinary ? "sudo" : null, binary, "build", "-t", tag, buildDir.toString()]
        cmdLine.removeAll([null])
        return executeAndWait(cmdLine)
    }

    @Override
    String buildImage(File buildDir, String tagWithoutVersion, List<String> tags) {
        Preconditions.checkArgument(tags as Boolean,  "Image tags can not be empty or null.")
        final tagsOptions = tags.collect { tag -> "-t ${tagWithoutVersion}:${tag}".split(' ') }
        final cmdLine = [isCentOSDockerBinary ? "sudo" : null, binary, "build", tagsOptions, buildDir.toString()].flatten()
        cmdLine.removeAll([null])
        return executeAndWait(cmdLine)
    }

    @Override
    String pushImage(String tag) {
        Preconditions.checkArgument(tag as Boolean,  "Image tag can not be empty or null.")
        def cmdLine = [isCentOSDockerBinary ? "sudo" : null, binary, "push", tag]
        cmdLine.removeAll([null])
        return executeAndWait(cmdLine)
    }

    private static String executeAndWait(List<String> cmdLine) {
        def process = cmdLine.execute()
        process.waitForProcessOutput(System.out, System.err)
        if (process.exitValue()) {
            throw new GradleException("Docker execution failed\nCommand line [${cmdLine}]")
        }
        return "Done"
    }

    @Override
    String run(String tag, String containerName, boolean detached, boolean autoRemove, 
            Map<String, String> env,
            Map<String, String> ports, Map<String, String> volumes, List<String> volumesFrom,
            List<String> links) {
        Preconditions.checkArgument(tag as Boolean,  "Image tag cannot be empty or null.")
        Preconditions.checkArgument(containerName as Boolean,  "Image name cannot be empty or null.")
        Preconditions.checkArgument(env != null,  "Environment map cannot be null.")
        Preconditions.checkArgument(ports != null,  "Exported port map cannot be null.")
        Preconditions.checkArgument(volumes != null,  "Volume map cannot be null.")
        Preconditions.checkArgument(volumesFrom != null,  "Volumes from list cannot be null.")
        Preconditions.checkArgument(links != null,  "Link list cannot be null.")
        Preconditions.checkArgument(!detached || !autoRemove,
            "Cannot set both detached and autoRemove options to true.");

        def detachedArg = detached ? '-d' : ''
        def removeArg = autoRemove ? '--rm' : ''
        def List<String> cmdLine = [isCentOSDockerBinary ? "sudo" : null, binary, "run", detachedArg, removeArg, "--name" , containerName]
        cmdLine.removeAll([null])
        cmdLine = appendArguments(cmdLine, env, "--env", '=')
        cmdLine = appendArguments(cmdLine, ports, "--publish")
        cmdLine = appendArguments(cmdLine, volumes, "--volume")
        cmdLine = appendArguments(cmdLine, volumesFrom, "--volumes-from")
        cmdLine = appendArguments(cmdLine, links, "--link")
        cmdLine.add(tag)
        return executeAndWait(cmdLine)
    }

    private static List<String> appendArguments(List<String> cmdLine, Map<String, String> map, String option,
            String separator = ':') {
        // Add each entry in the map as the indicated argument
        map.each { key, value ->
            cmdLine.add(option);
            cmdLine.add("${key}${separator}${value}")
        }
        return cmdLine
    }

    private static List<String> appendArguments(List<String> cmdLine, List<String> list, String option) {
        // Add each entry in the map as the indicated argument
        list.each {
            cmdLine.add(option);
            cmdLine.add(it);
        }
        return cmdLine
    }

}
