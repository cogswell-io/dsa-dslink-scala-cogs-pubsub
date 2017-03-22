# dsa-dslink-scala-cogs-pubsub
DSLink for Cogs Pub/Sub using the DSLink Scala SDK

You need to download the DSA and extract it into your home directory in order
for the deploy script to work seemlessly.

Updated dependencies for Scala IDE:
```
$ ./activator eclipse
```

Run all unit tests:
```
$ ./activator test
```

Build the zip file containing the DSLink:
```
$ ./activator dist
```

Deploy the DSLink into the DSA extracted to you home directory:
```
$ ./deploy.sh
```

