# Migrate from Genson to Jackson

Replace the `com.owlike:genson` JSON library with Jackson provided by Spring Boot.

## Steps
- Remove the Genson dependency from `pom.xml`.
- Annotate entities with Jackson annotations such as `@JsonProperty` and `@JsonIgnore`.
- Replace custom `JsonSerializer` implementations with Spring's `ObjectMapper`.
- Update tests and utilities that relied on Genson.
