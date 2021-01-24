def call(Map config) {
    pipeline {
        agent none
        stages {
            stage('Precheck') {
                steps {
                    sh 'docker -v'
                    script {
                        docker.image("mcr.microsoft.com/dotnet/sdk:${config.dotnetVersion}").inside {
                            sh 'ls'
                        }
                    }
                }
            }
            
            stage('Test') {
                steps {
                    script {
                        docker.image("mcr.microsoft.com/dotnet/sdk:${config.dotnetVersion}").inside {
                            sh 'dotnet test'
                        }    
                    }
                }
            }
            
            stage('Release Build') {
                steps {
                    script {
                        docker.image("mcr.microsoft.com/dotnet/sdk:${config.dotnetVersion}").inside {
                            sh "dotnet build ${config.projectName}/${config.projectName}.csproj --configuration Release -o output -r linux-x64"
                            writeFile encoding: 'UTF8', file: 'Dockerfile', text: """FROM mcr.microsoft.com/dotnet/runtime:5.0
COPY output /app
WORKDIR /app
ENTRYPOINT ["dotnet", "run", "${config.projectName}.dll"]"""
                        }
                    }
                    
                }
            }
            
            stage('Build & Push Docker Image') {
                steps {
                    sh "docker build . -t aksharpatel47/${config.dockerImageName ? config.dockerImageName : config.projectName}:latest"
                }
            }
        }
    }
}