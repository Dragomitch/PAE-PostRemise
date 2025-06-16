# Remove commons-pool2

Once HikariCP replaces commons-dbcp2, drop the transitive `commons-pool2` dependency.

## Steps
- Verify that no other library requires `commons-pool2`.
- Remove the dependency from `pom.xml` after migrating to HikariCP.
