#!/usr/bin/env bash

import hudson.model.Result
import hudson.model.Run
import jenkins.model.CauseOfInterruption.UserInterruption
import groovy.json.JsonOutput

def config = [:]

pipeline {
    agent any

    tools {
        maven 'default'
    }

    environment {
        tmpDir = "/tmp/katabuild/${BRANCH_NAME}_${BUILD_TIMESTAMP}"
    }

    stages {
        stage('Input') {
            steps {
                script {
                    if (BRANCH_NAME ==~ /.*release.*/) {
                        def input = input(id: 'buildConfig', message: 'Release?', parameters: [
                            string(name: "version", description: "Release version", defaultValue: "3.0.5")
                        ])
                        config.version = input
                    } else {
                        config.version = "3.0.5"
                    }
                }   
            }
        }

        stage('Prepare') {
            steps {
                script {
                    // Terminate running builds of the same job
                    abortPreviousBuilds()
                    sh "mkdir -p ${env.tmpDir}"
                    sh 'chmod -R 777 ${WORKSPACE}'
                }
            }
        }

        stage('Update version') {
            steps {
                echo "Version: ${config.version}"
                dir('source/com.kms.katalon') {
                    script {
                        def versionMapping = readFile(encoding: 'UTF-8', file: 'about.mappings')
                        versionMapping = versionMapping.replaceAll(/1=.*/, "1=${config.version}")
                        writeFile(encoding: 'UTF-8', file: 'about.mappings', text: versionMapping)
                        if (!fileExists('buildNumber.properties')) {
                            sh 'touch buildNumber.properties'
                        }
                        writeFile(encoding: 'UTF-8', file: 'buildNumber.properties', text: "buildNumber0=0")
                    }
                }
            }
        }
        
        stage('Building') {
                // Start maven commands to get dependencies
            steps {
                retry(3) {
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

                    script {
                        dir("source") {
                // Generate Katalon builds
                // If branch name contains "release", build production mode for non-qTest package
                // else build development mode for qTest package
                            if (BRANCH_NAME ==~ /.*release.*/) {
                                if (BRANCH_NAME ==~ /.*qtest.*/) {
                                    echo "Building: qTest Prod"
                                    sh 'mvn -pl \\!com.kms.katalon.product clean verify -P prod'
                                } else {
                                    echo "Building: Standard Prod"
                                    sh 'mvn -pl \\!com.kms.katalon.product.qtest_edition clean verify -P prod'
                                }

                            } else {
                                echo "Building: qTest Dev"
                                sh 'mvn -pl \\!com.kms.katalon.product clean verify -P dev'
                            }

                            // Generate API docs
                            sh "cd com.kms.katalon.apidocs && mvn clean verify && cp -R 'target/resources/apidocs' ${env.tmpDir}"
                        }
                    }
                }
            }
        }

        /*
        stage('Testing') {
            steps {
                dir ("source/com.kms.katalon.product.qtest_edition/target/products/com.kms.katalon.product.qtest_edition.product/macosx/cocoa/x86_64")
                {
                    sh 'curl -O https://github.com/katalon-studio/katalon-keyword-tests/archive/master.zip'
                    sh 'unzip master.zip'
                    sh './Katalon\\ Studio.app/Contents/MacOS/katalon -noSplash  -runMode=console -projectPath="katalon-keyword-tests/katalon-keyword-tests.prj" -retry=0 -testSuiteCollectionPath="Test Suites/All Tests -browserType=Chrome (headless)"'
                }
            }
        }
        */

        stage('Copy builds') {
            // Copy generated builds and changelogs to shared folder on server
            steps {
                dir("source/com.kms.katalon.product/target/products") {
                    script {
                        if (BRANCH_NAME ==~ /.*release.*/ && !(BRANCH_NAME ==~ /.*qtest.*/)) {
                            sh "cd com.kms.katalon.product.product/macosx/cocoa/x86_64 && cp -R 'Katalon Studio.app' ${env.tmpDir}"
                            writeFile(encoding: 'UTF-8', file: "${env.tmpDir}/changeLogs.txt", text: getChangeString())
                            writeFile(encoding: 'UTF-8', file: "${env.tmpDir}/commit.txt", text: "${GIT_COMMIT}")
                            fileOperations([
                                    fileCopyOperation(
                                        excludes: '',
                                        includes: '*.zip, *.tar.gz, *.app',
                                        flattenFiles: true,
                                        targetLocation: "${env.tmpDir}")
                            ])
            }
                    }
                }
                dir("source/com.kms.katalon.product.qtest_edition/target/products") {
                    script {
                        if (!(BRANCH_NAME ==~ /.*release.*/ && !(BRANCH_NAME ==~ /.*qtest.*/))) {
                            sh "cd com.kms.katalon.product.qtest_edition.product/macosx/cocoa/x86_64 && cp -R 'Katalon Studio.app' ${env.tmpDir}"
                            writeFile(encoding: 'UTF-8', file: "${env.tmpDir}/changeLogs.txt", text: getChangeString())
                            writeFile(encoding: 'UTF-8', file: "${env.tmpDir}/commit.txt", text: "${GIT_COMMIT}")
                            fileOperations([
                                fileCopyOperation(
                                        excludes: '',
                                        includes: '*.zip, *.tar.gz, *.app',
                                        flattenFiles: true,
                                        targetLocation: "${env.tmpDir}")
                            ])
                        }
                    }
                }
            }
        }

        stage('Package .DMG file') {
            steps {
            script {
                    // For release branches, execute codesign command to package .DMG file for macOS
            if (BRANCH_NAME ==~ /.*release.*/) {
                       sh "./package.sh ${env.tmpDir}"
            }
        }
            }
        }

        stage('Generate update packages') {
            steps {
                script {
                    if (BRANCH_NAME ==~ /.*release.*/ && !(BRANCH_NAME ==~ /.*qtest.*/) && !(BRANCH_NAME ==~ /.*beta.*/)) {
                        dir("tools/updater") {
                            def updateInfo = [
                                buildDir: "${WORKSPACE}/source/com.kms.katalon.product/target/products/com.kms.katalon.product.product",
                                destDir: "${tmpDir}/update",
                                version: "${config.version}"
                            ]
                            def json = JsonOutput.toJson(updateInfo)
                            json = JsonOutput.prettyPrint(json)
                            writeFile(file: 'scan_info.json', text: json)
                            sh 'java -jar json-map-builder-1.0.0.jar'
                        }
                    }
                }
            }
        }

        stage ('Success') {
            steps {
                script {
                    // Mark status for current job
                    currentBuild.result = 'SUCCESS'
                }
            }
        }
    }

    // Send notification emails
    post {
        changed {
            emailext  body: "Changes:\n " + getChangeString() + "\n\n Check console output at: $BUILD_URL/console" + "\n",
                recipientProviders: [brokenBuildSuspects(), developers()],
                subject: "Build $BUILD_NUMBER - " + currentBuild.currentResult + " ($JOB_NAME)",
                attachLog: true
        }
        success {
            mail(
                    from: 'build-ci@katalon.com',
                    replyTo: 'build-ci@katalon.com',
                    to: "qa@katalon.com",
                    subject: "Build $BUILD_NUMBER - " + currentBuild.currentResult + " ($JOB_NAME)",
                    body: "Changes:\n " + getChangeString() + "\n\n Check console output at: $BUILD_URL/console" + "\n"
            )
        }
    }

    // Configure Pipeline-specific options
    options {
        // Keep only last 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Timeout job after 60 minutes
        timeout(time: 60, unit: 'MINUTES')
        // Wait 10 seconds before starting scheduled build
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
