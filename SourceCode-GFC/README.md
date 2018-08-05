# gfc-code


## Quick build and test GFC

```java
$ mvn package
$ java -cp ./target/factchecking-1.0-SNAPSHOT-jar-with-dependencies.jar edu.wsu.eecs.gfc.exps.TestGFC ./sample_data/ ./output 0.01 0.0001 4 50
```

## Quick build and test OGFC

```java
$ mvn package
$ java -cp ./target/factchecking-1.0-SNAPSHOT-jar-with-dependencies.jar edu.wsu.eecs.gfc.exps.TestOGFC ./sample_data/ ./output 0.01 0.0001 4 50
```