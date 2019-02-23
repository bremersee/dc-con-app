pipeline {
  agent {
    label 'master'
  }
  stages {
    stage('Build') {
      steps {
        sh 'mvn clean compile'
      }
    }
    stage('Test') {
      steps {
        sh 'mvn test'
      }
    }
    stage('Site') {
      steps {
        sh 'mvn site-deploy'
      }
    }
  }
}