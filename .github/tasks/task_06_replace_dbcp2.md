# Replace commons-dbcp2 and Remove commons-codec

Adopt Spring Boot's default HikariCP connection pool and drop unused dependencies.

## Steps
- Replace `BasicDataSource` in `DalServicesImpl` with HikariCP via `spring-boot-starter-data-jpa`.
- Remove `commons-codec` if no direct usages remain.
