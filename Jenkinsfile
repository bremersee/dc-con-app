pipeline {
  agent none
  environment {
    SERVICE_NAME='dc-con-app'
    DOCKER_IMAGE='bremersee/dc-con-app'
    DEV_TAG='snapshot'
    PROD_TAG='latest'
    DEPLOY_SNAPSHOT=false
    DEPLOY_RELEASE=true
    PUSH_SNAPSHOT=false
    PUSH_RELEASE=true
    DEPLOY_DEMO=false
    SNAPSHOT_SITE=false
    RELEASE_SITE=true
  }
  stages {
    stage('Test') {
      agent {
        label 'maven'
      }
      tools {
        jdk 'jdk8'
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
    stage('Deploy snapshot') {
      agent {
        label 'maven'
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      when {
        allOf {
          branch 'develop'
          environment name: 'DEPLOY_SNAPSHOT', value: 'true'
        }
      }
      steps {
        sh 'mvn -B -DskipTests=true -Pdebian9,copy-to-and-install-on-dc,copy-to-and-install-on-dc2 deploy'
      }
    }
    stage('Deploy release') {
      agent {
        label 'maven'
      }
      when {
        allOf {
          branch 'master'
          environment name: 'DEPLOY_RELEASE', value: 'true'
        }
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh 'mvn -B -DskipTests=true -Dhttp.protocol.expect-continue=true -Pdebian9,deploy-to-repo-ubuntu-bionic deploy'
      }
      /*
      steps {
        sh 'mvn -B -DskipTests=true -Dhttp.protocol.expect-continue=true -Pdebian9,deploy-to-repo-ubuntu-bionic,apt-get-on-dc,apt-get-on-dc2 deploy'
      }
      */
    }
    stage('Push snapshot') {
      agent {
        label 'maven'
      }
      when {
        allOf {
          branch 'develop'
          environment name: 'PUSH_SNAPSHOT', value: 'true'
        }
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh '''
          mvn -B -DskipTests -Ddockerfile.skip=false package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=snapshot package dockerfile:push
          docker system prune -a -f
        '''
      }
    }
    stage('Push release') {
      agent {
        label 'maven'
      }
      when {
        allOf {
          branch 'master'
          environment name: 'PUSH_RELEASE', value: 'true'
        }
      }
      tools {
        jdk 'jdk8'
        maven 'm3'
      }
      steps {
        sh '''
          mvn -B -DskipTests -Ddockerfile.skip=false package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=latest package dockerfile:push
          docker system prune -a -f
        '''
      }
    }
    stage('Deploy demo on dev-swarm') {
      agent {
        label 'dev-swarm'
      }
      when {
        allOf {
          branch 'develop'
          environment name: 'PUSH_SNAPSHOT', value: 'true'
          environment name: 'DEPLOY_DEMO', value: 'true'
        }
      }
      steps {
        sh '''
          if docker service ls | grep -q ${SERVICE_NAME}; then
            echo "Updating service ${SERVICE_NAME} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            docker service update --image ${DOCKER_IMAGE}:${DEV_TAG} ${SERVICE_NAME}
          else
            echo "Creating service ${SERVICE_NAME} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            chmod 755 docker-swarm/service.sh
            docker-swarm/service.sh "${DOCKER_IMAGE}:${DEV_TAG}" "swarm,dev,demo"
          fi
        '''
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
        jdk 'jdk8'
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
        jdk 'jdk8'
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
        jdk 'jdk8'
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