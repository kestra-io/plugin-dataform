package io.kestra.plugin.dataform.cli;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.*;
import io.kestra.core.models.tasks.runners.ScriptService;
import io.kestra.core.models.tasks.runners.TaskRunner;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
import io.kestra.plugin.scripts.runner.docker.Docker;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Orchestrate a Dataform project"
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
        title = "The commands to run before main list of commands"
    )
    protected Property<List<String>> beforeCommands;

    @Schema(
        title = "The commands to run"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    @NotEmpty
    protected List<String> commands;

    @Schema(
        title = "Additional environment variables for the current process."
    )
    @PluginProperty(
        additionalProperties = String.class,
        dynamic = true
    )
    protected Map<String, String> env;

    @Schema(
        title = "Deprecated, use 'taskRunner' instead"
    )
    @PluginProperty
    @Deprecated
    private DockerOptions docker;

    @Schema(
        title = "The task runner to use.",
        description = "Task runners are provided by plugins, each have their own properties."
    )
    @PluginProperty
    @Builder.Default
    @Valid
    private TaskRunner<?> taskRunner = Docker.instance();

    @Schema(title = "The task runner container image, only used if the task runner is container-based.")
    @Builder.Default
    private Property<String> containerImage = Property.of(DEFAULT_IMAGE);

    private NamespaceFiles namespaceFiles;

    private Object inputFiles;

    private Property<List<String>> outputFiles;

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        var renderedOutputFiles = runContext.render(this.outputFiles).asList(String.class);
        var renderedBeforeCommands = runContext.render(this.beforeCommands).asList(String.class);
        return new CommandsWrapper(runContext)
            .withWarningOnStdErr(true)
            .withDockerOptions(injectDefaults(getDocker()))
            .withTaskRunner(this.taskRunner)
            .withContainerImage(runContext.render(this.containerImage).as(String.class).orElse(null))
            .withEnv(Optional.ofNullable(env).orElse(new HashMap<>()))
            .withNamespaceFiles(namespaceFiles)
            .withInputFiles(inputFiles)
            .withOutputFiles(renderedOutputFiles.isEmpty() ? null : renderedOutputFiles)
            .withCommands(
                ScriptService.scriptCommands(
                    List.of("/bin/sh", "-c"),
                    renderedBeforeCommands.isEmpty() ? null : renderedBeforeCommands,
                    runContext.render(this.commands)
                )
            )
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
