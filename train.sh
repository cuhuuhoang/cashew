#!/bin/bash

set -e

./gradlew shadowJar
java -cp build/libs/cashew-all.jar com.nut.cashew.bud.Ter
