set -e

mvn springdoc-openapi:generate

swagger-cli bundle target/openapi.json --dereference -o target/openapi-dereferenced.json

postman spec file update \
    6551e823-8c29-4648-bb26-f453de61e3e6 \
    openapi.json \
    < target/openapi-dereferenced.json