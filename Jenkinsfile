pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {
        stage('Build and test') {
            steps {
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
}