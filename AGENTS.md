---
name: "Erasmus-Management-Application"
description: "This repository contains a web application for managing Erasmus mobilities."
category: "Fullstack Development"
author: "Dragomitch"
authorUrl: "https://github.com/Dragomitch"
tags: ["Java", "Angular", "Web", "Maven", "GitHub", "CI/CD"]
lastUpdated: "2025-06-16"
---

# Erasmus Management Application (EMA)

## Project Overview

The Erasmus Management Application (EMA) is a fullstack web platform used to handle Erasmus mobility processes. The repository contains a Spring Boot backend and an Angular frontend. Use the dedicated guidelines for each part when contributing.

## Tech Stack

List the main technologies and tools used in the project:

- **Frontend**: Angular 20
- **Backend**: Spring Boot 3 (Java 21)
- **Database**: PostgreSQL
- **Deployment**: Docker Compose
- **Other Tools**: GitHub Actions, Maven, Node.js

For detailed instructions, see the stack-specific guides:

- [`backend/AGENTS.md`](backend/AGENTS.md) – Java backend
- [`frontend/AGENTS.md`](frontend/AGENTS.md) – Angular frontend

## Project Structure

Describe the recommended project directory structure:

```
project-name/
├── backend/
├── frontend/
├── SQLRessources/
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Development Guidelines

Refer to the backend or frontend guides for language-specific details. General rules include:

### Code Style

- Use consistent formatting tools
- Follow language-specific best practices
- Keep code clean and readable

### Naming Conventions

- File naming: kebab-case for Angular files, camelCase for Java classes
- Variable naming: camelCase
- Function naming: camelCase
- Class naming: PascalCase

### Git Workflow

- Branch naming conventions: `feature/<topic>` or `bugfix/<topic>`
- Commit message format: `<type>: <short description>`
- Pull Request process: open PRs against `master` with a clear summary

## Environment Setup

See the respective guides for setup requirements.

## Core Feature Implementation

Implementation details for each feature are explained in the stack-specific guides.

## Testing Strategy

Testing instructions vary between backend and frontend. Refer to the dedicated guides.

## Deployment Guide

The project is designed to run via Docker Compose. Each part has further notes in its own guide.

## Performance Optimization

Optimization techniques are described separately for the backend and frontend.

## Security Considerations

Security best practices are outlined in the backend and frontend guides.

## Monitoring and Logging

Monitoring and logging approaches are provided in the stack-specific guides.

## Common Issues

See the troubleshooting sections in each guide.

## Reference Resources

- [Official Documentation Link]
- [Related Tutorials]
- [Community Resources]
- [Best Practices Guide]

## Changelog

### v1.0.0 (YYYY-MM-DD)

- Initial release
- Implemented basic features

---

**Note**: Consult the backend or frontend guides depending on which part of the project you are working on.
