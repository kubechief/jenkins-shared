def call(Map config) {
    pipeline {
        agent any
        stages {
            stage('Precheck') {
                steps {
                    sh 'docker -v'
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
                            writeFile encoding: 'UTF8', file: 'Dockerfile', text: """FROM mcr.microsoft.com/dotnet/runtime:${config.dotnetVersion}
COPY output /app
WORKDIR /app
ENTRYPOINT ["dotnet", "run", "${config.projectName}.dll"]"""
                        }
                    }
                    
                }
            }
            
            stage('Build & Push Docker Image') {
                steps {
                    script {
                        docker.withRegistry('', 'docker-hub-cred') {
                           docker.build("aksharpatel47/${config.dockerImageName ? config.dockerImageName : config.projectName}:latest").push('latest')
                        }
                    }
                }
            }
        }
    }
}