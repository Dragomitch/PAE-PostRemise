# Replace jBCrypt with Spring PasswordEncoder

Use Spring Security's `PasswordEncoder` with a modern algorithm instead of `org.mindrot:jbcrypt`.

## Steps
- Add `spring-security-crypto` and configure a `PasswordEncoder` bean (e.g. Argon2 or Spring's BCrypt).
- Refactor `UserUccImpl` and `SessionUccImpl` to use the new encoder.
- Remove the `jbcrypt` dependency after migration.
