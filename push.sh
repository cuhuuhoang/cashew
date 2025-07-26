#!/bin/bash

set -e

git add -A
git commit -m $@
git push origin master
