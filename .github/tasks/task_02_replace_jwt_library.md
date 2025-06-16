# Replace JWT Library with Spring Security

Switch from `com.auth0:java-jwt` to Spring Security's JWT facilities.

## Steps
- Introduce Spring Security and configure JWT encoder and decoder beans.
- Refactor `SessionManager` to use Spring Security for token creation and validation.
- Remove the Auth0 dependency once migration is complete.
