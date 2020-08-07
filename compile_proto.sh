#!/bin/bash

OUTPUT_DIR=app/src/main/java/com/zackmathews/unifyidchallenge/proto/
PROTO_DIR=protolib/src/main/java/com/zackmathews/protolib/UnifyChallengeProto.proto
protoc --java_out=${OUTPUT_DIR} ${PROTO_DIR}
