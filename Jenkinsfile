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
            steps {
                sh 'docker compose --env-file .env.example build'
            }
        }
    }
}