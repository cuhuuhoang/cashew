#!/bin/bash

set -e

./gradlew shadowJar

count=${1:-1}
for ((i=1; i<=count; i++))
do
    java -cp build/libs/cashew-all.jar com.nut.cashew.bud.GameTrainer
done
