FROM jenkins/jenkins:lts-jdk17
USER root
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:27-cli /usr/local/libexec/docker/cli-plugins/ /usr/local/libexec/docker/cli-plugins/

RUN apt-get update && apt-get install -y --no-install-recommends zip unzip curl ca-certificates \
 && curl -sSL https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o /tmp/awscli.zip \
 && unzip -q /tmp/awscli.zip -d /tmp && /tmp/aws/install \
 && rm -rf /tmp/aws /tmp/awscli.zip /var/lib/apt/lists/*