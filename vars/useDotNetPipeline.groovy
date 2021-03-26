def call(Map config) {
    pipeline {
        agent any
        environment {
            DOTNET_CLI_HOME = "/tmp/DOTNET_CLI_HOME"
        }
        stages {
            stage('Precheck') {
                steps {
                    sh 'docker -v'
                    script {
                        docker.image("mcr.microsoft.com/dotnet/sdk:${config.dotnetVersion}-alpine").inside {
                            sh 'dotnet --info'
                        }
                    }
                }
            }
            
            stage('Test') {
                steps {
                    script {
                        docker.image("mcr.microsoft.com/dotnet/sdk:${config.dotnetVersion}-alpine").inside {
                            sh 'dotnet test'
                        }
                    }
                }
            }
            
            stage('Release Build') {
                steps {
                    script {
                        docker.image("mcr.microsoft.com/dotnet/sdk:${config.dotnetVersion}-alpine").inside {
                            sh "dotnet build ${config.projectName}/${config.projectName}.csproj --configuration Release -o output -r linux-x64"
                            writeFile encoding: 'UTF8', file: 'Dockerfile', text: """FROM mcr.microsoft.com/dotnet/runtime:${config.dotnetVersion}-alpine
COPY output /app
WORKDIR /app
ENTRYPOINT ["dotnet", "${config.projectName}.dll"]"""
                        }
                    }
                }
            }
            
            stage('Build & Push Docker Image') {
                when {
                    anyOf {
                        branch 'master';
                        branch 'develop';
                        branch 'release/\\w+';
                        branch 'hotfix/\\w+'
                    }
                    
                }
                steps {
                    script {
                        docker.withRegistry('', 'docker-hub-cred') {
                            def imageTag = getImageTag(env.BRANCH_NAME, env.BUILD_NUMBER)
                            def imageName = "aksharpatel47/${config.dockerImageName ? config.dockerImageName : currentBuild.projectName}:${imageTag}"
                            echo "Building image ${imageName}..."
                            def build = docker.build(imageName)
                            build.push("${imageTag}")
                        }
                    }
                }
            }
        }
    }
}

def getImageTag(branchName, buildNumber) {
    if (branchName.equals('master')) {
        def version = sh(returnStdout: true, script: "git tag --contains").trim()
        return "prod-${version}"
    } else if (branchName.equals('develop')) {
        return "dev-${buildNumber}"
    } else if (branchName.startsWith('Release') || branchName.startsWith('HotFix')) {
        def version = branchName.split("/")[1]
        return "qa-${version}-${buildNumber}"
    }
}