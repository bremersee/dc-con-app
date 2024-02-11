pipeline {
  agent {
    label 'docker && maven'
  }
  environment {
    CODECOV_TOKEN = credentials('dc-con-app-codecov-token')
    SNAPSHOT_SITE = false
    RELEASE_SITE = true
    DEPLOY_SNAPSHOT_ON_SERVER = false
    DEPLOY_RELEASE_ON_SERVER = false
    DEPLOY_RELEASE_ON_REPOSITORY_DEBIAN_BULLSEYE = false
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '8', artifactNumToKeepStr: '8'))
  }
  stages {
    stage('Tools') {
      tools {
        jdk 'jdk17'
        maven 'm3'
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
      }
    }
    stage('Build') {
      steps {
        sh 'echo "Maven Version ${POM_VERSION}"'
        sh 'mvn -B clean package'
      }
      post {
        always {
          junit '**/surefire-reports/*.xml'
          jacoco(
              execPattern: '**/coverage-reports/*.exec'
          )
        }
      }
    }
    stage('Deploy snapshot site') {
      when {
        allOf {
          environment name: 'SNAPSHOT_SITE', value: 'true'
          anyOf {
            branch 'develop'
            branch 'feature/*'
          }
        }
      }
      steps {
        sh 'mvn -B clean site-deploy'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Deploy release site') {
      when {
        allOf {
          branch 'main'
          environment name: 'RELEASE_SITE', value: 'true'
        }
      }
      steps {
        sh 'mvn -B -P gh-pages-site clean site site:stage scm-publish:publish-scm'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Deploy snapshot on servers') {
      when {
        allOf {
          branch 'develop'
          environment name: 'DEPLOY_SNAPSHOT_ON_SERVER', value: 'true'
        }
      }
      steps {
        sh 'mvn -B -DskipTests=true -Pdebian11,copy-to-and-install-on-dc3 clean deploy'
      }
    }
    stage('Deploy release on servers') {
      when {
        allOf {
          branch 'master'
          environment name: 'DEPLOY_RELEASE_ON_SERVER', value: 'true'
        }
      }
      steps {
        sh 'mvn -B -DskipTests=true -Pdebian11,copy-to-and-install-on-dc3 clean deploy'
      }
    }
    stage('Deploy release on apt repository debian-bullseye') {
      when {
        allOf {
          branch 'master'
          environment name: 'DEPLOY_RELEASE_ON_REPOSITORY_DEBIAN_BULLSEYE', value: 'true'
        }
      }
      steps {
        sh 'mvn -B -DskipTests=true -Dhttp.protocol.expect-continue=true -Pdebian11,deploy-to-repo-debian-bullseye clean deploy'
      }
    }
  }
}