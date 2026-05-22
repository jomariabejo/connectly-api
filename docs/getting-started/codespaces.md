# GitHub Codespaces Setup

This guide walks through running Connectly API in GitHub Codespaces with the same app flow you use locally.

## What Codespaces gives you

- A remote VS Code environment with the repository already cloned
- A terminal attached to the dev container
- Port forwarding for the API and supporting services

This repository does not currently include a `.devcontainer` configuration, so the steps below use the repo as-is.

## Open the repository in Codespaces

1. In GitHub, open the repository.
2. Click **Code**.
3. Select the **Codespaces** tab.
4. Create a new codespace from the `main` branch.

Wait for the workspace to finish loading before starting any services.

## Check the environment

Run these commands in the Codespaces terminal:

```bash
java -version
docker version
./gradlew --version
```

You need Java 17 and a working Docker daemon if you want to use the multi-environment Compose setup.

## Recommended workflow

The simplest path in Codespaces is the Docker Compose stack:

```bash
docker compose up -d --build
```

Then wait for the services to become healthy:

```bash
docker compose wait api-stage api-demo api-prod
docker compose ps
```

## Forward the ports

Forward these ports in Codespaces so the services are reachable from your browser and REST clients:

- `8081` for stage
- `8082` for demo
- `8083` for prod
- `8025` for Mailpit

If you run a local `bootRun` workflow instead, also forward `8080`.

## Verify the API

```bash
curl http://localhost:8081/api/actuator/health
curl http://localhost:8082/api/actuator/health
curl http://localhost:8083/api/actuator/health

curl http://localhost:8081/api/v1/public/hello
```

Expected responses:

- Health endpoints return `{"status":"UP"}`
- Hello endpoint returns `Hello, Connectly API is running!`

## Use the docs in Codespaces

Open these files from the workspace:

- [`src/main/resources/docs/postman/connectly-api-v1.postman_collection.json`](../../src/main/resources/docs/postman/connectly-api-v1.postman_collection.json)
- [`src/main/resources/docs/curls/auth-crud.md`](../../src/main/resources/docs/curls/auth-crud.md)
- [`src/main/resources/docs/curls/curls-test.http`](../../src/main/resources/docs/curls/curls-test.http)

Postman is still the easiest way to manage variables like `baseUrl`, `userToken`, and `verificationToken` while working from Codespaces.

## If Docker is not available

If your Codespaces configuration does not provide Docker, use a minimal local-only workflow:

1. Start or point the app at a reachable PostgreSQL instance.
2. Run `./gradlew bootRun`.
3. Forward port `8080`.
4. Use `http://localhost:8080/api` as the base URL.

## Troubleshooting

- If port forwarding does not appear, manually add the port in the Ports panel.
- If the app fails to start, check the terminal for the first stack trace above `BUILD FAILED`.
- If Compose hangs, re-run `docker compose ps` and wait for the health checks to pass.
