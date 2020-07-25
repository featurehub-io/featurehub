#!/bin/sh
pub run test_coverage
genhtml -o coverage coverage/lcov.info
