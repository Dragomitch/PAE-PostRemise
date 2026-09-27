# Build the Angular frontend and serve it with Nginx
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
# The localized production build (angular.json: localize: true) writes one app per locale to
# dist/<project>/browser/<locale>/ (fr/ and en/, each with its own <base href>); nginx.conf
# redirects / to the preferred locale and gives each locale its own SPA fallback.
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
