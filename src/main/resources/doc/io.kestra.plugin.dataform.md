# How to use the Dataform plugin

Run Dataform CLI commands — compile projects, execute workflows, and manage transformations — from Kestra flows inside a container.

## Common properties

`containerImage` defaults to `dataformco/dataform:latest`. `taskRunner` controls where the container runs — defaults to Docker. GCP credentials are not plugin-level properties; pass them via `env` (set `GOOGLE_APPLICATION_CREDENTIALS` to a service account JSON file name) and supply the JSON content via `inputFiles`. Provide the Dataform project config (project ID and BigQuery region) via `inputFiles` as `.df-credentials.json`.

## Tasks

`cli.DataformCLI` runs one or more Dataform CLI commands set in `commands` (e.g. `dataform run`, `dataform compile`). Use `beforeCommands` for setup steps such as `npm install @dataform/core`. Pass supporting files — service account JSON, project config, playbook YAML — via `inputFiles` or pull them from [namespace files](https://kestra.io/docs/concepts/namespace-files). Pair with a preceding `git.Clone` task inside a `WorkingDirectory` to run against a Dataform project checked out from a repository.
