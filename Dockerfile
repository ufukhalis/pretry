FROM adoptopenjdk/openjdk11
RUN mkdir /data
VOLUME /data
ADD /target/project-pretry-*.jar app.jar
ENTRYPOINT ["java", "-jar","/app.jar"]
