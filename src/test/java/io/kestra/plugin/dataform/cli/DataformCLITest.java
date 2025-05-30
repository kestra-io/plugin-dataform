package io.kestra.plugin.dataform.cli;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
class DataformCLITest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @SuppressWarnings("unchecked")
    void run() throws Exception {
        String environmentKey = "MY_KEY";
        String environmentValue = "MY_VALUE";

        DataformCLI.DataformCLIBuilder<?, ?> dataformBuilder = DataformCLI.builder()
            .id(IdUtils.create())
            .type(DataformCLI.class.getName())
            .docker(DockerOptions.builder().image("dataformco/dataform:latest").entryPoint(Collections.emptyList()).build())
            .commands(Property.ofValue(List.of("dataform --version")));

        DataformCLI runner = dataformBuilder.build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, runner, Map.of("environmentKey", environmentKey, "environmentValue", environmentValue));

        ScriptOutput scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));

        runner = dataformBuilder
            .env(Map.of("{{ inputs.environmentKey }}", "{{ inputs.environmentValue }}"))
            .beforeCommands(Property.ofValue(List.of(
                "dataform init postgres new_project",
                "cd new_project"
            )))
            .commands(Property.ofValue(List.of(
                "echo \"::{\\\"outputs\\\":{" +
                    "\\\"customEnv\\\":\\\"$" + environmentKey + "\\\"" +
                    "}}::\"",
                "dataform compile | tr -d ' \n' | xargs -0 -I {} echo '::{\"outputs\":{}}::'"
            )))
            .build();

        scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));
        assertThat(scriptOutput.getVars().get("customEnv"), is(environmentValue));
    }
}