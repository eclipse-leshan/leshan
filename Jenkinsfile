pipeline {
  agent {
    label 'basic'
  }
  tools {
    maven 'apache-maven-latest'
    jdk 'adoptopenjdk-hotspot-jdk8-latest'
  }
  options {
    timeout(time: 30, unit: 'MINUTES')
    disableConcurrentBuilds()
  }
  stages {
    stage('Checkout') {
      steps {
        checkout poll: false, scm: scmGit(branches: [[name: '*/master']], userRemoteConfigs: [[url: 'https://github.com/eclipse/leshan.git']])
      }
    }
    stage('Maven build') {
      steps {
        sh '''
          mvn clean install javadoc:javadoc -PeclipseJenkins -B
        '''
      }
    }
    stage('Sortpom') {
      steps {
        sh '''
          mvn com.github.ekryd.sortpom:sortpom-maven-plugin:verify -PallPom -B
        '''
      }
    }
    stage('Copy artifacts') {
      steps {
        sh '''
          cp leshan-server-demo/target/leshan-server-demo-*-jar-with-dependencies.jar leshan-server-demo.jar
          cp leshan-bsserver-demo/target/leshan-bsserver-demo-*-jar-with-dependencies.jar leshan-bsserver-demo.jar
          cp leshan-client-demo/target/leshan-client-demo-*-jar-with-dependencies.jar leshan-client-demo.jar
        '''
      }
    }
  }
  post {
    unsuccessful {
      mail to: 'sbernard@sierrawireless.com', subject: 'Build $BUILD_STATUS $PROJECT_NAME #$BUILD_NUMBER!', body: '''Check console output at $BUILD_URL to view the results.'''
    }
    fixed {
      mail to: 'sbernard@sierrawireless.com', subject: 'Build $BUILD_STATUS $PROJECT_NAME #$BUILD_NUMBER!', body: '''Check console output at $BUILD_URL to view the results.'''
    }
    always {
      junit '**/target/surefire-reports/*.xml'
      archiveArtifacts artifacts: 'leshan-server-demo.jar,leshan-bsserver-demo.jar,leshan-client-demo.jar'
    }
  }
}
