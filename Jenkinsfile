import hudson.model.Result
import hudson.model.Run
import jenkins.model.CauseOfInterruption.UserInterruption

pipeline {
    agent any
   
    tools {
        maven 'default'
    }
    
    stages {
        stage('Prepare') {
            steps {
                script {
                    abortPreviousBuilds()          
                }
            }
        }

        stage('Set permissions to source') {
            steps {
                sh '''chmod -R 777 ${WORKSPACE}'''
            }
        }

        stage('Building') {
            // start maven commands to get dependencies
            steps {
                sh 'ulimit -c unlimited'
                sh 'cd source/com.kms.katalon.repo && mvn p2:site'
                sh 'cd source/com.kms.katalon.repo && nohup mvn -Djetty.port=9999 jetty:run > /tmp/9999.log &'
                sh '''
                    until $(curl --output /dev/null --silent --head --fail http://localhost:9999/site); do
                        printf '.'
                        cat /tmp/9999.log
                        sleep 5
                    done
                '''

                sh 'cd source/com.kms.katalon.p2site && nohup mvn -Djetty.port=33333 jetty:run > /tmp/33333.log &'
                sh '''
                    until $(curl --output /dev/null --silent --head --fail http://localhost:33333/site); do
                        printf '.'
                        cat /tmp/33333.log
                        sleep 5
                    done
                '''
                
             // generate katalon builds   
                script {
                    dir("source") {
                        if (BRANCH_NAME ==~ /^[release]+/) {
                            sh ''' mvn clean verify -P prod '''
                        } else {                      
                            sh ''' mvn -pl \\!com.kms.katalon.product clean verify -P dev '''
                        }
                    }
                }
            }
        }

        stage('Copy builds') {
            // copy generated builds and changelogs to shared folder on server
            steps {
                dir("source/com.kms.katalon.product.qtest_edition/target/products") {
                    script {
                        String tmpDir = "/tmp/katabuild/${BRANCH_NAME}_${BUILD_TIMESTAMP}"
                        writeFile(encoding: 'UTF-8', file: "${tmpDir}/${BRANCH_NAME}_${BUILD_TIMESTAMP}_changeLogs.txt", text: getChangeString())
                        // copy builds, require https://wiki.jenkins.io/display/JENKINS/File+Operations+Plugin
                        fileOperations([
                                fileCopyOperation(
                                        excludes: '',
                                        includes: '*.zip, *.tar.gz',
                                        flattenFiles: true,
                                        targetLocation: "${tmpDir}")
                        ])
                    }
                }
            }
        }

        stage ('Success') {
            steps {
                script {
                    currentBuild.result = 'SUCCESS'
                }
            }
        }
    }

    post {
        always {
            mail(
                    from: 'build-ci@katalon.com',
                    replyTo: 'build-ci@katalon.com',
                    to: "qa@katalon.com",
                    subject: "Build $BUILD_NUMBER - " + currentBuild.currentResult + " ($JOB_NAME)",
                    body: "Changes:\n " + getChangeString() + "\n\n Check console output at: $BUILD_URL/console" + "\n"
            )
        }
    }

    // configure Pipeline-specific options
    options {
        // keep only last 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // timeout job after 60 minutes
        timeout(time: 60, unit: 'MINUTES')
        // wait 10 seconds before starting scheduled build
        quietPeriod 10
    }
}

def abortPreviousBuilds() {
    Run previousBuild = currentBuild.rawBuild.getPreviousBuildInProgress()
    
    while (previousBuild != null) {
        if (previousBuild.isInProgress()) {
            def executor = previousBuild.getExecutor()
            if (executor != null) {
                echo ">> Aborting older build #${previousBuild.number}"
                executor.interrupt(Result.ABORTED, new UserInterruption(
                    "Aborted by newer build #${currentBuild.number}"
                ))
            }
        }
        previousBuild = previousBuild.getPreviousBuildInProgress()
    }
}

@NonCPS
def getChangeString() {
    MAX_MSG_LEN = 100
    String changeString = ""
    echo "Gathering SCM changes"
    def changeLogSets = currentBuild.rawBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            truncated_msg = entry.msg.take(MAX_MSG_LEN)
            changeString += " - ${truncated_msg} [${entry.author}]\n"
        }
    }

    if (!changeString) {
        changeString = " - No new changes"
    }
    return changeString
}
