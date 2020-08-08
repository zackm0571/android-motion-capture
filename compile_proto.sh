#!/bin/bash

OUTPUT_DIR=app/src/main/java/com/zackmatthews/unifyidchallenge/proto
PROTO_DIR=protolib/src/main/java/com/zackmathews/protolib/UnifyChallengeProto.proto
mkdir ${OUTPUT_DIR}
protoc --java_out=${OUTPUT_DIR} ${PROTO_DIR}
