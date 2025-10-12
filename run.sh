#!/bin/bash

set -e

./gradlew shadowJar
java -cp build/libs/cashew-all.jar com.nut.cashew.bud.GameTrainer
java -cp build/libs/cashew-all.jar com.nut.cashew.Game
