FROM openjdk:11
ADD springboot-seata/order-server/target/order-server.jar order-server.jar
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "order-server.jar"]