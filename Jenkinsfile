pipeline {
  agent none
  environment {
    DEPLOY_SNAPSHOT_ON_SERVER = false
    DEPLOY_RELEASE_ON_SERVER = true
    DEPLOY_RELEASE_ON_REPOSITORY_DEBIAN_BULLSEYE = true
    SNAPSHOT_SITE = false
    RELEASE_SITE = true
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '8', artifactNumToKeepStr: '8'))
  }
  stages {
    stage('Test') {
      agent {
        label 'maven'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      when {
        not {
          branch 'feature/*'
        }
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
        sh 'mvn -B clean test'
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
    stage('Deploy snapshot on servers') {
      agent {
        label 'maven'
      }
      when {
        allOf {
          branch 'develop'
          environment name: 'DEPLOY_SNAPSHOT_ON_SERVER', value: 'true'
        }
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'mvn -B -DskipTests=true -Pdebian11,copy-to-and-install-on-dc3 clean deploy'
      }
    }
    stage('Deploy release on servers') {
      agent {
        label 'maven'
      }
      when {
        allOf {
          branch 'master'
          environment name: 'DEPLOY_RELEASE_ON_SERVER', value: 'true'
        }
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'mvn -B -DskipTests=true -Pdebian11,copy-to-and-install-on-dc3 clean deploy'
      }
    }
    stage('Deploy release on apt repository debian-bullseye') {
      agent {
        label 'maven'
      }
      when {
        allOf {
          branch 'master'
          environment name: 'DEPLOY_RELEASE_ON_REPOSITORY_DEBIAN_BULLSEYE', value: 'true'
        }
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'mvn -B -DskipTests=true -Dhttp.protocol.expect-continue=true -Pdebian11,deploy-to-repo-debian-bullseye clean deploy'
      }
    }
    stage('Deploy snapshot site') {
      agent {
        label 'maven'
      }
      environment {
        CODECOV_TOKEN = credentials('dc-con-app-codecov-token')
      }
      when {
        allOf {
          branch 'develop'
          environment name: 'SNAPSHOT_SITE', value: 'true'
        }
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'mvn -B site-deploy'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Deploy release site') {
      agent {
        label 'maven'
      }
      environment {
        CODECOV_TOKEN = credentials('dc-con-app-codecov-token')
      }
      when {
        allOf {
          branch 'master'
          environment name: 'RELEASE_SITE', value: 'true'
        }
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'mvn -B -P gh-pages-site site site:stage scm-publish:publish-scm'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Test feature') {
      agent {
        label 'maven'
      }
      when {
        branch 'feature/*'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
        sh 'mvn -B -P feature,allow-features clean test'
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
  }
}