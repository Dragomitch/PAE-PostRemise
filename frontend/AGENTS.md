---
name: "Erasmus-Management-Application Frontend"
description: "Guidelines for the Angular client."
category: "Frontend Development"
author: "Dragomitch"
authorUrl: "https://github.com/Dragomitch"
tags: ["Angular", "TypeScript", "Node.js"]
lastUpdated: "2025-06-16"
---

# EMA Frontend Guide

## Project Overview

This document covers the development workflow for the Angular frontend of the Erasmus Management Application. The frontend consumes the REST API exposed by the backend and provides the user interface.

## Tech Stack

List the main technologies and tools used in the project:

- **Framework**: Angular 20
- **Language**: TypeScript
- **Package Manager**: npm
- **Styling**: CSS
- **Testing**: Karma & Jasmine

## Project Structure

```
frontend/
├── src/
│   ├── app/
│   ├── assets/
│   └── ...
├── angular.json
├── package.json
├── tsconfig.json
└── Dockerfile
```

## Development Guidelines

### Code Style

- Use the Angular CLI formatting tools (`ng lint`).
- Prefer single quotes and end files with a newline.
- Keep components small and focused.

### Naming Conventions

- File naming: kebab-case (`my-component.ts`).
- Variable naming: camelCase.
- Function naming: camelCase.
- Class naming: PascalCase.

### Git Workflow

- Use `feature/` or `bugfix/` branches off `master`.
- Commit messages follow `<type>: <short description>`.
- Submit Pull Requests with screenshots for UI changes when possible.

## Environment Setup

### Development Requirements

- Node.js 20+
- npm 10+
- Angular CLI 20

### Installation Steps

```bash
# 1. Navigate to the frontend folder
cd frontend

# 2. Install dependencies
npm install

# 3. Start development server
ng serve
```

## Core Feature Implementation

### Feature Module 1

Components are located under `src/app`. Services handle API calls via Angular's `HttpClient`.

```typescript
// Example code
@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}
  getUsers() {
    return this.http.get<User[]>('/api/users');
  }
}
```

### Feature Module 2

Routing is defined in `app.routes.ts` using Angular's RouterModule. Guards can protect routes based on authentication state.

## Testing Strategy

### Unit Testing

- Testing framework: Jasmine with Karma
- Test coverage requirements: aim for 80%
- Test file organization: mirror component structure with `*.spec.ts` files

### Integration Testing

- Test scenarios: component interaction with services
- Testing tools: Angular TestBed

### End-to-End Testing

- Test workflow: run `ng e2e` using your preferred e2e framework
- Automation tools: Protractor or Cypress

## Deployment Guide

### Build Process

```bash
ng build --configuration production
```

### Deployment Steps

1. Build the Docker image
2. Serve the static files via Nginx or the Node-based server
3. Configure environment variables for API endpoints
4. Verify the application loads on port 4200

### Environment Variables

```env
API_URL=
```

## Performance Optimization

### Frontend Optimization

- Code splitting via lazy-loaded modules
- Use Angular's `ChangeDetectionStrategy.OnPush` where appropriate
- Cache HTTP responses

## Security Considerations

### Data Security

- Validate user input on the client when possible
- Escape dynamic content to avoid XSS

### Authentication & Authorization

- Tokens are stored in memory or session storage
- Route guards enforce authorization rules

## Monitoring and Logging

### Application Monitoring

- Use browser dev tools to track performance
- Integrate with monitoring services if available

### Log Management

- Use `console.log` sparingly and remove debug statements in production builds

## Common Issues

### Issue 1: Dependency errors after a pull

**Solution**: Delete `node_modules` and run `npm install` again.

### Issue 2: API requests fail in development

**Solution**: Check that the backend is running and `API_URL` is correctly configured.

## Reference Resources

- [Angular Documentation](https://angular.dev/docs)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/handbook/)

## Changelog

### v1.0.0 (YYYY-MM-DD)

- Initial frontend release
- Basic UI components implemented

---
