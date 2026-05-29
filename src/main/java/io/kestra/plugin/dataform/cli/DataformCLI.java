package io.kestra.plugin.dataform.cli;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.*;
import io.kestra.core.models.tasks.runners.TaskRunner;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
import io.kestra.plugin.scripts.runner.docker.Docker;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Run Dataform CLI commands in Kestra",
    description = "Executes Dataform CLI commands through the configured task runner. Defaults to a container run with image `dataformco/dataform:latest`; override with taskRunner or containerImage. Supports setup commands, custom env, and namespace/input/output files."
)
@Plugin(
    examples = {
        @Example(
            title = "Compile and run a Dataform project from Git",
            full = true,
            code = {
                """
                    id: dataform
                    namespace: company.team
                    tasks:
                      - id: wdir
                        type: io.kestra.plugin.core.flow.WorkingDirectory
                        tasks:
                          - id: clone_repo
                            type: io.kestra.plugin.git.Clone
                            url: https://github.com/dataform-co/dataform-example-project-bigquery

                          - id: transform
                            type: io.kestra.plugin.dataform.cli.DataformCLI
                            beforeCommands:
                              - npm install @dataform/core
                              - dataform compile
                            env:
                              GOOGLE_APPLICATION_CREDENTIALS: "sa.json"
                            inputFiles:
                              sa.json: "{{ secret('GCP_SERVICE_ACCOUNT_JSON') }}"
                              .df-credentials.json: |
                                {
                                  "projectId": "<gcp-project-id>",
                                  "location": "us"
                                }
                            commands:
                              - dataform run --dry-run
                    """
            }
        )
    }
)
public class DataformCLI extends Task implements RunnableTask<ScriptOutput>, NamespaceFilesInterface, InputFilesInterface, OutputFilesInterface {
    private static final String DEFAULT_IMAGE = "dataformco/dataform:latest";

    @Schema(
        title = "Run setup commands first",
        description = "Optional commands executed before the main commands in the same working directory and environment."
    )
    @PluginProperty(group = "execution")
    protected Property<List<String>> beforeCommands;

    @Schema(
        title = "Run Dataform CLI commands",
        description = "Required commands executed sequentially with `/bin/sh -c`; include your Dataform CLI actions here."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<List<String>> commands;

    @Schema(
        title = "Additional environment variables",
        description = "Key-value map merged into the process environment; supports templated values; defaults to an empty map."
    )
    @PluginProperty(group = "execution", 
        additionalProperties = String.class,
        dynamic = true
    )
    protected Map<String, String> env;

    @Schema(
        title = "Deprecated Docker runner settings",
        description = "Legacy DockerOptions field; prefer taskRunner. If provided without image or entryPoint, defaults to `dataformco/dataform:latest` and an empty entrypoint."
    )
    @PluginProperty(group = "deprecated")
    @Deprecated
    private DockerOptions docker;

    @Schema(
        title = "Select task runner implementation",
        description = "Defaults to the Docker runner; plugin task runners may expose their own properties."
    )
    @PluginProperty(group = "execution")
    @Builder.Default
    @Valid
    private TaskRunner<?> taskRunner = Docker.instance();

    @Schema(
        title = "Container image for task runner",
        description = "Used only for container-based task runners; defaults to `dataformco/dataform:latest`."
    )
    @Builder.Default
    @PluginProperty(group = "execution")
    private Property<String> containerImage = Property.ofValue(DEFAULT_IMAGE);

    @PluginProperty(group = "source")
    private NamespaceFiles namespaceFiles;

    @PluginProperty(group = "source")
    private Object inputFiles;

    @PluginProperty(group = "destination")
    private Property<List<String>> outputFiles;

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        var renderedOutputFiles = runContext.render(this.outputFiles).asList(String.class);
        return new CommandsWrapper(runContext)
            .withWarningOnStdErr(true)
            .withDockerOptions(injectDefaults(getDocker()))
            .withTaskRunner(this.taskRunner)
            .withContainerImage(runContext.render(this.containerImage).as(String.class).orElse(null))
            .withEnv(Optional.ofNullable(env).orElse(new HashMap<>()))
            .withNamespaceFiles(namespaceFiles)
            .withInputFiles(inputFiles)
            .withOutputFiles(renderedOutputFiles.isEmpty() ? null : renderedOutputFiles)
            .withInterpreter(Property.ofValue(List.of("/bin/sh", "-c")))
            .withBeforeCommands(this.beforeCommands)
            .withCommands(this.commands)
            .run();
    }

    private DockerOptions injectDefaults(DockerOptions original) {
        if (original == null) {
            return null;
        }

        var builder = original.toBuilder();
        if (original.getImage() == null) {
            builder.image(DEFAULT_IMAGE);
        }
        if (original.getEntryPoint() == null || original.getEntryPoint().isEmpty()) {
            builder.entryPoint(List.of(""));
        }

        return builder.build();
    }

}
