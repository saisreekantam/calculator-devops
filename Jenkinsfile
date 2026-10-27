pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()          // two deploys must never race on the same stack
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 20, unit: 'MINUTES')
    }

    triggers {
        githubPush()                        // fires when a GitHub webhook reaches Jenkins
        pollSCM('H/2 * * * *')              // fallback: Jenkins on localhost is not reachable by GitHub
    }

    environment {
        // Jenkins runs as a macOS launchd service with a minimal PATH
        JAVA_HOME     = '/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home'
        PATH          = "${JAVA_HOME}/bin:/usr/local/apache-maven-3.9.9/bin:/Applications/Docker.app/Contents/Resources/bin:/usr/local/bin:/opt/homebrew/bin:${env.PATH}"

        DOCKER_IMAGE  = 'saivenkat1507/calculator-api'
        IMAGE_TAG     = "${env.BUILD_NUMBER}"
        PROXY_PORT    = '8085'
        NOTIFY_EMAIL  = 'saivenkatsreekantam@gmail.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --pretty="format:%h %an: %s"'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                // A failing test makes mvn exit non-zero, which fails the build here
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build \
                      -t ${DOCKER_IMAGE}:${IMAGE_TAG} \
                      -t ${DOCKER_IMAGE}:latest \
                      .
                '''
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_TOKEN')]) {
                    // Throwaway docker config inside the workspace: the login token is
                    // written there (not to the macOS keychain) and deleted right after.
                    // An empty config also drops Docker Desktop's "desktop-linux" context,
                    // so the daemon socket has to be given explicitly.
                    sh '''
                        export DOCKER_HOST="unix://$HOME/.docker/run/docker.sock"
                        export DOCKER_CONFIG="$WORKSPACE/.docker"
                        mkdir -p "$DOCKER_CONFIG" && echo '{}' > "$DOCKER_CONFIG/config.json"
                        echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin
                        docker push ${DOCKER_IMAGE}:${IMAGE_TAG}
                        docker push ${DOCKER_IMAGE}:latest
                        docker logout
                        rm -rf "$DOCKER_CONFIG"
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                // `up -d` only recreates containers whose image/config changed,
                // so re-running the pipeline is safe against a live stack.
                sh 'docker compose up -d --no-build --remove-orphans --wait --wait-timeout 120'
                sh 'docker compose ps'
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    for i in $(seq 1 20); do
                      if curl -sf http://localhost:${PROXY_PORT}/actuator/health | grep -q '"status":"UP"'; then
                        echo "Health check passed"; break
                      fi
                      echo "Waiting for app... ($i)"; sleep 3
                    done
                    curl -sf http://localhost:${PROXY_PORT}/actuator/health | grep -q '"status":"UP"'

                    RESP=$(curl -sf "http://localhost:${PROXY_PORT}/api/v1/add?a=2&b=3")
                    echo "Smoke test: $RESP"
                    echo "$RESP" | grep -q '"result":5'

                    CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${PROXY_PORT}/api/v1/divide?a=1&b=0")
                    echo "Divide by zero returned HTTP $CODE"
                    test "$CODE" = "400"
                '''
            }
        }
    }

    post {
        success {
            emailext(
                to: "${NOTIFY_EMAIL}",
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                body: """<p>Build <b>#${env.BUILD_NUMBER}</b> of <b>${env.JOB_NAME}</b> succeeded.</p>
                         <p>Image: <code>${DOCKER_IMAGE}:${IMAGE_TAG}</code> pushed to Docker Hub and deployed.</p>
                         <p>API: http://localhost:${PROXY_PORT}/api/v1/add?a=2&amp;b=3</p>
                         <p><a href="${env.BUILD_URL}">Open build</a></p>"""
            )
        }
        failure {
            emailext(
                to: "${NOTIFY_EMAIL}",
                subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                attachLog: true,
                body: """<p>Build <b>#${env.BUILD_NUMBER}</b> of <b>${env.JOB_NAME}</b> failed.</p>
                         <p>Failed stage details are in the attached console log.</p>
                         <p><a href="${env.BUILD_URL}console">Open console</a></p>"""
            )
        }
        always {
            sh 'rm -rf "$WORKSPACE/.docker" || true'
        }
    }
}
