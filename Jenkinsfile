pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        AWS_DEFAULT_REGION = 'us-east-1'
        EB_APP   = 'medic'
        EB_ENV   = 'medic-env'
        SERVICES = 'gateway-service identity-service booking-service notification-service audit-service'
    }

    stages {
        stage('Build and test') {
            steps {
                script {
                    env.IMAGE_TAG = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
                }
                sh './mvnw -B verify'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml'
                }
            }
        }

        stage('Build images') {
            environment {
                POSTGRES_PASSWORD         = 'ci-build-only'
                MEDIC_DB_PASSWORD         = 'ci-build-only'
                IDENTITY_DB_PASSWORD      = 'ci-build-only'
                BOOKING_DB_PASSWORD       = 'ci-build-only'
                NOTIFICATION_DB_PASSWORD  = 'ci-build-only'
                AUDIT_DB_PASSWORD         = 'ci-build-only'
                JWT_SECRET                = 'Y2ktYnVpbGQtb25seS1ub3QtYS1yZWFsLXNlY3JldC1rZXktMTIzNDU2Nzg5MA=='
                INTERNAL_API_KEY          = 'ci-build-only-not-a-real-key'
            }
            steps {
                sh 'docker compose --env-file .env.example build'
            }
        }

        stage('Push to ECR') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-deployer',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        set -e
                        ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
                        REGISTRY=$ACCOUNT.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
                        aws ecr get-login-password | docker login --username AWS --password-stdin $REGISTRY
                        for s in $SERVICES; do
                          docker tag $s:$IMAGE_TAG $REGISTRY/medic/$s:$IMAGE_TAG
                          docker push $REGISTRY/medic/$s:$IMAGE_TAG
                        done
                    '''
                }
            }
        }

        stage('Deploy to Beanstalk') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-deployer',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        set -e
                        ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
                        REGISTRY=$ACCOUNT.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
                        BUCKET=elasticbeanstalk-$AWS_DEFAULT_REGION-$ACCOUNT
                        rm -rf bundle medic-*.zip && mkdir bundle
                        sed -e "s#__REGISTRY__#$REGISTRY#g" -e "s#__TAG__#$IMAGE_TAG#g" \
                            deploy/docker-compose.prod.yml > bundle/docker-compose.yml
                        cp -r docker/postgres bundle/postgres-init
                        cp -r docker/kafka bundle/kafka
                        (cd bundle && zip -qr ../medic-$IMAGE_TAG.zip .)
                        aws s3 cp medic-$IMAGE_TAG.zip s3://$BUCKET/medic/medic-$IMAGE_TAG.zip
                        aws elasticbeanstalk create-application-version --application-name $EB_APP \
                            --version-label $IMAGE_TAG-$BUILD_NUMBER \
                            --source-bundle S3Bucket=$BUCKET,S3Key=medic/medic-$IMAGE_TAG.zip
                        aws elasticbeanstalk update-environment --environment-name $EB_ENV --version-label $IMAGE_TAG-$BUILD_NUMBER
                        DONE=no
                        for i in $(seq 1 80); do
                          OUT=$(aws elasticbeanstalk describe-environments --environment-names $EB_ENV \
                                --query 'Environments[0].[Status,VersionLabel]' --output text)
                          if echo "$OUT" | grep -q '^Ready' && echo "$OUT" | grep -q "$IMAGE_TAG"; then DONE=yes; break; fi
                          sleep 15
                        done
                        [ "$DONE" = yes ] || { echo "Deployment did not finish with version $IMAGE_TAG"; exit 1; }
                    '''
                }
            }
        }

        stage('Verify') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-deployer',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        URL=$(aws elasticbeanstalk describe-environments --environment-names $EB_ENV \
                              --query 'Environments[0].CNAME' --output text)
                        for i in $(seq 1 30); do
                          curl -fsS http://$URL/actuator/health && exit 0
                          sleep 10
                        done
                        echo "Health check failed"; exit 1
                    '''
                }
            }
        }
    }
}