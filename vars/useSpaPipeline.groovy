def call(Map config) {
    pipeline {
        agent any
        stages {
            stage('Precheck') {
                steps {
                    sh 'docker -v'
                    script {
                        docker.image("node:lts-alpine").inside {
                            sh 'node --version'
                        }
                    }
                }
            }

            stage('NPM Install') {
                steps {
                    script {
                        docker.image("node:lts-alpine").inside {
                            sh 'npm install'
                        }
                    }
                }
            }
            
            stage('Test') {
                steps {
                    script {
                        docker.image("node:lts-alpine").inside {
                            sh 'npm run test'
                        }    
                    }
                }
            }
            
            stage('Release Build') {
                steps {
                    script {
                        docker.image("node:lts-alpine").inside {
                            sh "npm run build:prod"
                            writeFile encoding: 'UTF8', file: 'Dockerfile', text: """FROM nginx:1.19-alpine
COPY dist /usr/share/nginx/html
"""
                        }
                    }
                }
            }
            
            stage('Build & Push Docker Image') {
                when {
                    branch 'master'
                }
                steps {
                    script {
                        docker.withRegistry('', 'docker-hub-cred') {
                            def imageName = "aksharpatel47/${config.dockerImageName ? config.dockerImageName : currentBuild.projectName}:${env.BUILD_NUMBER}";
                            echo "Building image ${imageName}..."
                            def build = docker.build(imageName)
                            build.push("${env.BUILD_NUMBER}")
                            build.push("latest")
                        }
                    }
                }
            }
        }
    }
}
