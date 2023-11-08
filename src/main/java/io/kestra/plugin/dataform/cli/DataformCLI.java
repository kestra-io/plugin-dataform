package io.kestra.plugin.dataform.cli;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.NamespaceFiles;
import io.kestra.core.models.tasks.NamespaceFilesInterface;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.RunnerType;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
import io.kestra.plugin.scripts.exec.scripts.services.ScriptService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.kestra.core.utils.Rethrow.throwFunction;

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
                namespace: dev
                tasks:
                  - id: wdir
                    type: io.kestra.core.tasks.flows.WorkingDirectory
                    tasks:
                      - id: clone_repo
                        type: io.kestra.plugin.git.Clone
                        url: https://github.com/dataform-co/dataform-example-project-bigquery                

                      - id: transform
                        type: io.kestra.plugin.dataform.cli.DataformCLI
                        beforeCommands:
                          - dataform compile
                        commands:
                          - dataform run --dry-run
                """
            }
        )
    }
)
public class DataformCLI extends Task implements RunnableTask<ScriptOutput>, NamespaceFilesInterface {
    private static final String DEFAULT_IMAGE = "dataformco/dataform:latest";

    @Schema(
        title = "The commands to run before main list of commands"
    )
    @PluginProperty(dynamic = true)
    protected List<String> beforeCommands;

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
        title = "Docker options when for the `DOCKER` runner",
        defaultValue = "{image=" + DEFAULT_IMAGE + ", pullPolicy=ALWAYS}"
    )
    @PluginProperty
    @Builder.Default
    protected DockerOptions docker = DockerOptions.builder().build();

    private NamespaceFiles namespaceFiles;

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        return new CommandsWrapper(runContext)
            .withWarningOnStdErr(true)
            .withRunnerType(RunnerType.DOCKER)
            .withDockerOptions(injectDefaults(getDocker()))
            .withEnv(Optional.ofNullable(env).orElse(new HashMap<>()))
            .withNamespaceFiles(namespaceFiles)
            .withCommands(
                ScriptService.scriptCommands(
                    List.of("/bin/sh", "-c"),
                    Optional.ofNullable(this.beforeCommands).map(throwFunction(runContext::render)).orElse(null),
                    runContext.render(this.commands)
                                            )
                         )
            .run();
    }

    private DockerOptions injectDefaults(DockerOptions original) {
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
