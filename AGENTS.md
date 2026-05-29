# Kestra Dataform Plugin

## What

- Provides plugin components under `io.kestra.plugin.dataform.cli`.
- Includes classes such as `DataformCLI`.

## Why

- What user problem does this solve? Teams need to plugin Dataform for Kestra from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Dataform steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Dataform.

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
