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
        sh 'mvn -DskipTests=true -Pdebian9,copy-to-and-install-on-dc,copy-to-and-install-on-dc2 deploy'
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
    stage('Snapshot Site') {
      when {
        branch 'develop'
      }
      steps {
        sh 'mvn site-deploy'
      }
    }
    stage('Release Site') {
      when {
        branch 'master'
      }
      steps {
        sh 'mvn -P gh-pages-site site site:stage scm-publish:publish-scm'
      }
    }
  }
}