#!/bin/bash

TYPE=${1:-LINKED}
WARMUP=${2:-1000000}
MEASUREMENTS=${3:-5000000}

java -verbose:gc -XX:+AlwaysPreTouch -Xms4g -Xmx4g -XX:NewSize=512m -XX:MaxNewSize=1024m -cp target/classes:target/coralpool-all.jar com.coralblocks.coralpool.bench.ObjectPoolGrowthBench1 $TYPE $WARMUP $MEASUREMENTS

