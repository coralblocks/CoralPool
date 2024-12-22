#!/bin/bash

WARMUP=${1:-1000000}
MEASUREMENTS=${2:-5000000}

java -verbose:gc -XX:+AlwaysPreTouch -Xms4g -Xmx4g -XX:NewSize=512m -XX:MaxNewSize=1024m -cp target/classes:target/coralpool-all.jar com.coralblocks.coralpool.bench.ObjectPoolGrowthBench1 $WARMUP $MEASUREMENTS

