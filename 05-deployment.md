# Sprint 1 — Deployment (Render, Free Tier)

Render is the pick here: free web service, free managed PostgreSQL, no credit card required to start, and it builds directly from a Dockerfile or auto-detects Maven/Gradle.

## Heads-up before you start
- Spring Boot's default JVM footprint (150–300MB+ idle) is close to Render's free-tier 512MB RAM cap. Cap the heap explicitly (see JVM flags below) so the app doesn't get OOM-killed under load.
- The free web service **spins down after ~15 minutes of inactivity** and takes 30–60s to wake on the next request. Fine for a course demo — just mention it if you're presenting live so it isn't mistaken for a bug.
- Render's free Postgres instance currently comes with a fixed lifetime (check Render's current free-tier terms before demo day — these limits change) — back up your schema/seed script so you can recreate the DB if it expires mid-semester.

## 1. Dockerfile (recommended — most predictable build)
```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-Xmx256m -Xss512k"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

## 2. `application.properties` — read config from environment
Never hardcode DB credentials or the JWT secret. Use env vars with local defaults:
```properties
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/online_learning}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:postgres}
spring.jpa.hibernate.ddl-auto=update
server.port=${PORT:8080}

app.jwt.secret=${JWT_SECRET:dev-only-secret-change-me}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}
```

## 3. Create the Render Postgres instance
1. Render dashboard → New → PostgreSQL → pick the free plan.
2. Once created, Render gives you an **Internal Database URL** and an **External Database URL**, plus username/password separately.
3. Note these — you'll paste them into your web service's environment variables.

## 4. Create the Render Web Service
1. Render dashboard → New → Web Service → connect your GitHub repo.
2. Environment: **Docker** (it'll pick up the Dockerfile above automatically), or choose "Java" runtime and let Render auto-detect Maven if you'd rather skip the Dockerfile.
3. Set environment variables:
   - `DATABASE_URL` → the Postgres instance's internal connection string (in JDBC form: `jdbc:postgresql://<host>:<port>/<dbname>`)
   - `DATABASE_USERNAME`, `DATABASE_PASSWORD` → from the Postgres instance
   - `JWT_SECRET` → a long random string (generate one, don't reuse the dev default)
4. Deploy. Render builds the Dockerfile and gives you a public URL like `https://your-app.onrender.com`.

## 5. Frontend (React) deployment
If the React app is a separate project, deploy it on Vercel or Netlify (both free for static sites) and point its API base URL at the Render backend URL. Remember to add the frontend's deployed origin to Spring Security's CORS config:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://your-frontend.vercel.app", "http://localhost:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("*"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

## 6. Sanity-check checklist before demo day
- [ ] `JWT_SECRET` is set via env var, not committed to the repo
- [ ] Hitting the Render URL cold (after it's slept) still works — just slow the first time
- [ ] CORS allows your deployed frontend's exact origin
- [ ] Postgres free-tier expiry date (if any) is noted somewhere your team will see it before it lapses

## Alternative: Fly.io
If Render's free Postgres terms don't work for your timeline, Fly.io is a solid fallback: free allowance of 3 shared-CPU VMs (256MB RAM each) plus a free Postgres instance with 3GB storage. It requires the `flyctl` CLI and a `fly.toml` instead of a dashboard-driven setup — more control, slightly more setup work than Render.
