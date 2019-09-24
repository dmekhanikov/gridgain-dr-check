# Data Center Replication validation tool

This is a simple tool, that can be used to verify GridGain cache contents in different data centers.
It's primary goal is testing of the Data Center Replication feature of GridGain.

# Building and running

Run the following command in order to build the project:

```bash
$ mvn clean package
```

In the `target` directory a file `dr-check.jar` will appear. After that you'll be able to go to `target` and run the 
tool:
```bash
$ java -jar dr-check.jar ../config.yml
```

If any mismatch is found, then an exception will be reported with the following message: 
`Cache contents in different data centers don't match`.

## Configuration
`config.yml` contains an example of what the configuration may look like.

It consists of two lists `data-centers` and `caches`. 

First one is a list of addresses that can be used to connect to the data centers using GridGain thin Java clients.

`caches` is a list of cache names, that need to be validated.

## Key and value classes

If data is stored in `BinaryObject` format, then it won't be deserialized, so data classes won't be needed.
But if `OptimizedMarshaller` or `JdkMarshaller` is used or data classes implement `Externalizable` interface, then data 
classes need to be provided to the classpath of the tool:
```bash
$ java -cp <path-to-data-classes.jar> dr-check.jar org.gridgain.dr.DrCheck ../config.yml
```
