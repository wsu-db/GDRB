FROM openjdk:8-jdk-alpine AS GFC

# Get required software packages
RUN apk update && apk add git && apk add maven

# Get GFC
RUN git clone 'https://github.com/vaibhavc99/GDRB.git'
WORKDIR /GDRB/SourceCode-GFC

# Build package
RUN mvn package

# Execute GFC
CMD java -Xmx2048m -cp ./target/factchecking-1.0-SNAPSHOT-jar-with-dependencies.jar edu.wsu.eecs.gfc.exps.TestGFC ./sample_data/ ./output 0.01 0.0001 4 50
