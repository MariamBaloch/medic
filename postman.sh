set -e

mvn springdoc-openapi:generate

swagger-cli bundle target/openapi.json --dereference -o target/openapi-dereferenced.json

postman spec file update \
    45a28d2c-4007-42eb-88ac-250f4761ad0a \
    openapi.json \
    < target/openapi-dereferenced.json