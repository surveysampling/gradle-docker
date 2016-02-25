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

import groovy.util.logging.Log
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

@Log
class ExampleProjects {

    @Test
    public void simpleExample() {
        def projectDir = new File('examples/simple')
        def expectedDockerfile = new File(projectDir, 'Dockerfile.expected').text

        def actualDockerfile = runGradleTask(projectDir, 'docker')

        assertThat actualDockerfile, is(equalTo(expectedDockerfile))
    }

    @Test
    public void applicationExample() {
        def projectDir = new File('examples/application')
        def expectedDockerfile = new File(projectDir, 'Dockerfile.expected').text

        def actualDockerfile = runGradleTask(projectDir, 'distDocker')

        assertThat actualDockerfile, is(equalTo(expectedDockerfile))
    }

    private String runGradleTask(File projectDir, String taskName) {
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect();
        try {
            connection.newBuild()
                    .forTasks(taskName)
                    .run();
        } finally {
            connection.close();
        }
        return new File(projectDir, 'build/docker/Dockerfile').text
    }
}

