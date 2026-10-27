# calculator-devops

A Calculator REST API in Spring Boot with an automated pipeline around it:
**GitHub → Jenkins → Maven build and tests → Docker image → Docker Hub → Docker Compose deploy (Nginx + 2 replicas)**.

CSE 816 Software Production Engineering mini-project.

| | |
|---|---|
| Language / build | Java 21, Spring Boot 3.5, Maven |
| Docker Hub image | [`saivenkat1507/calculator-api`](https://hub.docker.com/r/saivenkat1507/calculator-api) |
| Deployed URL (local) | http://localhost:8085 (Nginx reverse proxy) |

## API

| Operation | Endpoint | Example response |
|---|---|---|
| Add | `GET /api/v1/add?a=2&b=3` | `{"operation":"add","a":2,"b":3,"result":5}` |
| Subtract | `GET /api/v1/subtract?a=10&b=4` | `{"operation":"subtract","a":10,"b":4,"result":6}` |
| Multiply | `GET /api/v1/multiply?a=6&b=7` | `{"operation":"multiply","a":6,"b":7,"result":42}` |
| Divide | `GET /api/v1/divide?a=10&b=4` | `{"operation":"divide","a":10,"b":4,"result":2.5}` |
| Health | `GET /actuator/health` | `{"status":"UP",...}` |

Errors (division by zero, missing or non-numeric parameters) return **HTTP 400** with a structured body:

```json
{"timestamp":"2026-10-27T16:01:57Z","status":400,"error":"Bad Request",
 "message":"Division by zero is not allowed","path":"/api/v1/divide"}
```

Numbers are handled as `BigDecimal`, so `0.1 + 0.2` returns `0.3`.

## Build, test, run

Requires JDK 21+ and Maven 3.9+.

```bash
mvn clean package -DskipTests      # build target/calculator-api.jar
mvn test                           # JUnit 5 unit + MockMvc tests, reports in target/surefire-reports/
java -jar target/calculator-api.jar  # serves on http://localhost:8080
```

## Docker

```bash
docker build -t saivenkat1507/calculator-api:latest .
docker run --rm -p 8086:8080 saivenkat1507/calculator-api:latest
curl "http://localhost:8086/api/v1/add?a=2&b=3"
```

The Dockerfile has two stages. A `maven:3.9.9-eclipse-temurin-21` stage downloads dependencies in their own cached layer, then builds the JAR. A slim `eclipse-temurin:21-jre-alpine` stage copies only that JAR and runs it as a non-root `app` user, with `EXPOSE 8080` and a `HEALTHCHECK`.

## Docker Compose

```bash
docker compose up -d --wait        # Nginx proxy + 2 API replicas
docker compose ps
curl -i "http://localhost:8085/api/v1/add?a=2&b=3"   # X-Upstream header shows which replica answered
docker compose down
```

- `calculator`: the API, 2 replicas. Not published to the host; reachable only through the proxy. Has a health check on `/actuator/health`, a restart policy and environment variables.
- `proxy`: `nginx:1.27-alpine`, published on `${PROXY_PORT:-8085}`. It starts only after the API is healthy and round-robins between the replicas using Docker DNS.
- Both services share the `calc-net` bridge network.

Variables: `DOCKER_IMAGE`, `IMAGE_TAG` (Jenkins sets this to the build number), `PROXY_PORT`.

## Jenkins pipeline

The `Jenkinsfile` is a declarative pipeline with these stages:

1. **Checkout**: pulls the commit that triggered the build.
2. **Build**: `mvn clean package -DskipTests`.
3. **Test**: `mvn test`. JUnit XML results are published, and any failing test fails the build.
4. **Docker Build**: builds the image tagged `:<BUILD_NUMBER>` and `:latest`.
5. **Push**: pushes both tags to Docker Hub. The login comes from the Jenkins credential `dockerhub-creds`, in a throwaway Docker config.
6. **Deploy**: `docker compose up -d --wait`. Only changed containers are recreated, so the pipeline is safe to re-run.
7. **Verify**: checks health through Nginx, runs an `add` smoke test, and expects HTTP 400 on divide-by-zero.

The pipeline is triggered by a GitHub webhook (`githubPush`), with Poll SCM every 2 minutes as a fallback. Success and failure emails are sent through Email Extension.

### One-time Jenkins setup

1. Plugins: Pipeline, Git, GitHub, JUnit, Credentials Binding, Email Extension, Timestamper.
2. Add a credential under *Manage Jenkins → Credentials*: **Username with password**, ID `dockerhub-creds`, with your Docker Hub username and an access token.
3. Configure SMTP under *Manage Jenkins → System → Extended E-mail Notification*: `smtp.gmail.com`, port 465, SSL, and a Gmail App Password stored as a credential.
4. Create a new *Pipeline* job using *Pipeline script from SCM*, pointing at this repo, branch `*/main`, script path `Jenkinsfile`.

## Repository layout

```
├── src/main/java/com/spe/calculator   # controller, service, DTOs, exception handler
├── src/test/java/com/spe/calculator   # JUnit 5 + MockMvc + @SpringBootTest
├── pom.xml
├── Dockerfile / .dockerignore
├── docker-compose.yml
├── nginx/nginx.conf
├── Jenkinsfile
└── .gitignore
```
