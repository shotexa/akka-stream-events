FROM openjdk:11 AS build
WORKDIR /docker-build-stage
RUN curl -fLo cs https://git.io/coursier-cli-"$(uname | tr LD ld)" && \
    chmod +x cs && \
    ./cs install sbt
ENV PATH="/root/.local/share/coursier/bin:${PATH}"
RUN rm ./cs
COPY . /docker-build-stage    
RUN sbt compile && sbt assembly 

FROM openjdk:11 AS run
WORKDIR /docker-run-stage
COPY --from=build /docker-build-stage/target/scala-2.13/bet-history-assembly-0.0.1-SNAPSHOT.jar /docker-run-stage/app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]


