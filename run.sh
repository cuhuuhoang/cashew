#!/bin/bash

./gradlew shadowJar && java -cp build/libs/cashew-all.jar com.nut.cashew.Game
