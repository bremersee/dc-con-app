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
    stage('Deploy snapshot') {
      when {
        branch 'develop'
      }
      steps {
        sh 'mvn -DskipTests=true -Pdebian9,copy-to-and-install-on-dc deploy'
      }
    }
    stage('Deploy release') {
      when {
        branch 'master'
      }
      steps {
        sh 'mvn -DskipTests=true -Dhttp.protocol.expect-continue=true -Pdebian9,deploy-to-repo-ubuntu-bionic,apt-get-on-dc,apt-get-on-dc2 deploy'
      }
    }
    stage('Site') {
      when {
        anyOf {
          branch 'develop'
          branch 'master'
        }
      }
      steps {
        sh 'mvn site-deploy'
      }
    }
  }
}