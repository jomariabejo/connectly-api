#!/bin/sh
set -e
# Render Postgres provides DATABASE_URL as postgresql://... JDBC expects jdbc:postgresql://...
if [ -n "${DATABASE_URL:-}" ]; then
  export SPRING_DATASOURCE_URL=$(printf '%s' "$DATABASE_URL" | sed 's|^postgresql:|jdbc:postgresql:|')
fi
exec java -jar /app/app.jar "$@"
