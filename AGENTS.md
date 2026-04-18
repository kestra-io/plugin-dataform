# Kestra Dataform Plugin

## What

- Provides plugin components under `io.kestra.plugin.dataform.cli`.
- Includes classes such as `DataformCLI`.

## Why

- This plugin integrates Kestra with Dataform CLI.
- It provides tasks that orchestrate Dataform through CLI commands.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `dataform`

### Key Plugin Classes

- `io.kestra.plugin.dataform.cli.DataformCLI`

### Project Structure

```
plugin-dataform/
├── src/main/java/io/kestra/plugin/dataform/cli/
├── src/test/java/io/kestra/plugin/dataform/cli/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
