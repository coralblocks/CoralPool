#!/bin/bash

TYPE=${1:-LinkedObjectPool}
WARMUP=${2:-1000000}
MEASUREMENTS=${3:-2000000}

java -verbose:gc -XX:+AlwaysPreTouch -Xms4g -Xmx4g -XX:NewSize=512m -XX:MaxNewSize=1024m -cp target/classes:target/coralpool-all.jar com.coralblocks.coralpool.bench.ObjectPoolBench1 $TYPE $WARMUP $MEASUREMENTS

