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
package se.transmode.gradle.plugins.docker

import groovy.transform.TupleConstructor
import groovy.util.logging.Log
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import se.transmode.gradle.plugins.docker.image.Dockerfile

@TupleConstructor
@Log
class LegacyDockerfileMethods implements GroovyInterceptable {

    private static Logger logger = Logging.getLogger(LegacyDockerfileMethods)
    Dockerfile dockerfile

    def invokeMethod(String name, args) {
        logger.warn('The {} method has been deprecated and is scheduled to be removed. Use the new dockerfile DSL instead.', name)
        metaClass.getMetaMethod(name, args).invoke(this, args)
    }

    /**
     * Set the default command of the Docker image ('CMD' in Dockerfile). Deprecated.
     *
     * Use the new dockerfile DSL instead:
     *   dockerfile {
     *     cmd ['your', 'command']
     *   }
     *
     */
    @Deprecated
    void setDefaultCommand(List cmd) {
        dockerfile.cmd(cmd)
    }

    /**
     * Set the default command of the Docker image ('CMD' in Dockerfile). Deprecated.
     *
     * Use the new dockerfile DSL instead:
     *   dockerfile {
     *     cmd ['your', 'command']
     *   }
     *
     */
    @Deprecated
    void defaultCommand(List cmd) {
        dockerfile.cmd(cmd)
    }

    /**
     * Set the entry point of the Docker image ('ENTRYPOINT' in Dockerfile. Deprectated.
     *
     * Use the new dockerfile DSL instead:
     *   dockerfile {
     *     entrypoint ['your', 'command']
     *   }
     *
     */
    @Deprecated
    void entryPoint(List entryPoint) {
        dockerfile.entrypoint(cmd)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void workingDir(String wd) {
        dockerfile.workdir(wd)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void runCommand(String command) {
        dockerfile.run(command)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void exposePort(Integer... ports) {
        dockerfile.expose(*ports)
    }

    @Deprecated
    void exposePort(String port) {
        dockerfile.expose(port)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void switchUser(String userName) {
        dockerfile.user(userName)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void setEnvironment(String key, String value) {
        dockerfile.env(key, value)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void addInstruction(String cmd, String value) {
        dockerfile."${cmd}"(value)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void volume(String... paths) {
        dockerfile.volume(*paths)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void addFile(String source, String destination='/') {
        dockerfile.add(source, destination)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void addFile(File source, String destination='/') {
        dockerfile.add(source, destination)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void addFile(URL source, String destination='/') {
        dockerfile.add(source, destination)
    }

    /**
     * This method is deprecated. Use the new Dockerfile DSL instead:
     */
    @Deprecated
    void addFile(Closure copySpec) {
        dockerfile.add(copySpec)
    }

}
