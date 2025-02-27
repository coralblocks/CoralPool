#!/bin/bash

TYPE=${1:-LINKED}
CAPACITY=${2:-100}
PRELOAD=${3:-100}
PASSES=${4:-500}

java -verbose:gc -XX:+AlwaysPreTouch -Xms4g -Xmx4g -XX:NewSize=512m -XX:MaxNewSize=1024m -cp target/classes:target/coralpool-all.jar com.coralblocks.coralpool.bench.ObjectPoolGrowthBench $TYPE $CAPACITY $PRELOAD $PASSES

