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
import com.google.common.annotations.VisibleForTesting
import com.google.common.io.Files
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import se.transmode.gradle.plugins.docker.client.DockerClient
import se.transmode.gradle.plugins.docker.client.NativeDockerClient
import se.transmode.gradle.plugins.docker.image.Dockerfile

class DockerTask extends DockerTaskBase {

    private static Logger logger = Logging.getLogger(DockerTask)
    public static final String DEFAULT_IMAGE = 'ubuntu'

    // Name and Email of the image maintainer
    String maintainer
    // Whether or not to execute docker to build the image (default: false)
    Boolean dryRun
    // Whether or not to push the image into the registry (default: false)
    Boolean push
    // Hostname, port of the docker image registry unless Docker Registry Hub is used
    String registry
    // What to tag versions the created docker image with e.g. latest
    List<String> tagVersions

    @Delegate(deprecated=true)
    LegacyDockerfileMethods legacyMethods

    // fixme: all of this should work: dockerfile = File()/String() and dockerfile { ... } how can I achieve this?
    private Dockerfile dockerfile

    @Override
    Task configure(Closure configureClosure) {
        def resolveClosure = { path -> project.file(path) }
        def copyClosure = { Closure copyClosure -> project.copy(copyClosure) }
        this.dockerfile = new Dockerfile(stageDir, resolveClosure, copyClosure)
        this.legacyMethods = new LegacyDockerfileMethods(dockerfile)
        super.configure(configureClosure)
    }

    /**
     * Configure Dockerfile with a closure, e.g.:
     *   dockerfile {
     *       FROM 'ubuntu'
     *       ADD myFile
     *   }
     * @param closure to configure dockerfile
     */
    public void dockerfile(Closure closure) {
        dockerfile.with(closure)
    }

    /**
     * Start off with existing external Dockerfile and extend it
     * @param Path to external Dockerfile
     */
    public void setDockerfile(String path) {
        dockerfile(project.file(path))
    }

    /**
     * Start off with existing external Dockerfile and extend it
     * @param External Dockerfile
     */
    public void setDockerfile(File baseFile) {
        logger.info('Creating Dockerfile from file {}.', baseFile)
        dockerfile.extendDockerfile(baseFile)
    }

    /**
     * Name of base docker image
    */
    String baseImage
    /**
     * Return the base docker image.
     *
     * If the base image is set in the task, return it. Otherwise return the base image
     * defined in the 'docker' extension. If the extension base image is not set determine
     * base image based on the 'targetCompatibility' property from the java plugin.
     *
     * @return Name of base docker image
     */
    public String getBaseImage() {
        def defaultImage = project.hasProperty('targetCompatibility') ? JavaBaseImage.imageFor(project.targetCompatibility).imageName : DEFAULT_IMAGE
        return baseImage ?: (project[DockerPlugin.EXTENSION_NAME].baseImage ?: defaultImage)
    }


    // Dockerfile instructions (ADD, RUN, etc.)
    def instructions
    // Dockerfile staging area i.e. context dir
    File stageDir
    DockerTask() {
        instructions = []
        stageBacklog = []
        tagVersions = []
        applicationName = project.name
        stageDir = new File(project.buildDir, "docker")
    }
    
    void addFile(String source, String destination='/') {
        addFile(project.file(source), destination)
    }

    void addFile(File source, String destination='/') {
        def target = stageDir
        if (source.isDirectory()) {
            target = new File(stageDir, source.name)
        }
        stageBacklog.add { ->
            project.copy {
                from source
                into target
            }
        }
        instructions.add("ADD ${source.name} ${destination}")
    }

    void addFile(Closure copySpec) {
        final tarFile = new File(stageDir, "add_${instructions.size()+1}.tar")
        stageBacklog.add { ->
            createTarArchive(tarFile, copySpec)
        }
        instructions.add("ADD ${tarFile.name} ${'/'}")
    }

    void createTarArchive(File tarFile, Closure copySpec) {
        final tmpDir = Files.createTempDir()
        logger.info("Creating tar archive {} from {}", tarFile, tmpDir)
        /* copy all files to temporary directory */
        project.copy {
            with {
                into('/') {
                    with copySpec
                }
            }
            into tmpDir
        }
        /* create tar archive */
        new AntBuilder().tar(
                destfile: tarFile,
                basedir: tmpDir
        )
    }

    void workingDir(String wd) {
        instructions.add("WORKDIR ${wd}")
    }

    void addInstruction(String cmd, String value) {
        instructions.add("${cmd} ${value}")
    }

    void runCommand(String command) {
        instructions.add("RUN ${command}")
    }

    void exposePort(Integer... ports) {
        instructions.add('EXPOSE ' + ports.join(' ').trim())
    }

    void setEnvironment(String key, String value) {
        instructions.add("ENV ${key} ${value}")
    }

    void setTagVersion(String version) {
        tagVersion = version;
    }

    void setTagVersionToLatest() {
        tagVersion = 'latest';
    }

    void tagVersions(List<String> tagVersions) {
        this.setTagVersions(tagVersions)
    }

    void setTagVersions(List<String> tagVersions) {
        this.tagVersions = tagVersions
    }

    void volume(String... paths) {
        instructions.add('VOLUME ["' + paths.join('", "') + '"]')
    }

    void setEntryPoint(List entryPoint) {
        instructions.add('ENTRYPOINT ["' + entryPoint.join('", "') + '"]')
    }

    void entryPoint(List entryPoint) {
        this.setEntryPoint(entryPoint)
    }

    void setDefaultCommand(List cmd) {
        instructions.add('CMD ["' + cmd.join('", "') + '"]')
    }

    void defaultCommand(List cmd) {
        this.setDefaultCommand(cmd)
    }

    void contextDir(String contextDir) {
        stageDir = new File(stageDir, contextDir)
    }

    protected File createDirIfNotExists(File dir) {
        if (!dir.exists())
            dir.mkdirs()
        return dir
    }

    @VisibleForTesting
    protected void setupStageDir() {
        logger.info('Setting up staging directory.')
        createDirIfNotExists(stageDir)
        dockerfile.stagingBacklog.each() { closure -> closure() }
    }

    @VisibleForTesting
    protected Dockerfile buildDockerfile() {
        if (!dockerfile.hasBase()) {
            def baseImage = getBaseImage()
            logger.info('Creating Dockerfile from base {}.', baseImage)
            dockerfile.from(baseImage)
        }
        // fixme: only add maintainer if not already set in external dockerfile or via dockerfile.maintainer
        if (getMaintainer()) {
            dockerfile.maintainer(getMaintainer())
        }
        return dockerfile.appendAll(instructions)
    }

    @TaskAction
    void build() {
        setupStageDir()
        buildDockerfile().writeToFile(new File(stageDir, 'Dockerfile'))
        final tagWithoutVersion = this.tag
        tag = getImageTag()
        logger.info('Determining image tag: {}', tag)

        if (!dryRun) {
            DockerClient client = getClient()


            final isEmpty = tagVersions.isEmpty()
            if(isEmpty) {
            println client.buildImage(stageDir, tag)
            } else {
                println client.buildImage(stageDir, tagWithoutVersion, tagVersions)
            }
            if (push) {
                if(isEmpty) {
                println client.pushImage(tag)
                } else {
                    tagVersions.each { taggedVersion ->
                        println client.pushImage("${tagWithoutVersion}:${taggedVersion}")
            }
        }
            }
        }

    }

    private String getDefaultImageTag() {
        String tag
        if (registry) {
            tag = "${-> registry}/${-> applicationName}"
        } else if (project.group) {
            tag = "${-> project.group}/${-> applicationName}"
        } else {
            tag = "${-> applicationName}"
        }
        return tag
    }

    private String appendImageTagVersion(String tag) {
        def version = tagVersion ?: project.version
        if(version == 'unspecified') {
            version = 'latest'
        }
        //FIXME
        if(!tagVersions.isEmpty()) {
            tagVersions.each { tagVersion ->
                if(tagVersion != 'latest') {
                    version = tagVersion
                }
            }
        }
        return "${tag}:${version}"

    }
}
