# Update Logging to SLF4J/Logback

Replace the custom `LogManager` and `java.util.logging` setup with Spring Boot's default SLF4J + Logback stack.

## Steps
- Use `org.slf4j.Logger` via `LoggerFactory` throughout the codebase.
- Remove the `commons-logging` dependency and related configuration.
- Configure log levels in `application.properties` or `logback-spring.xml`.
