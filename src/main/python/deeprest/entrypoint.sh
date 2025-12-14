#!/bin/bash
set -e

cleanup() {
    kill -KILL "$PYTHON_PID" "$JAVA_PID" 2>/dev/null || true
    exit 0
}

trap cleanup SIGINT SIGTERM EXIT

echo "Starting DeepREST Python component..."
python3 deeprest.py &
PYTHON_PID=$!

sleep 2

echo "Starting RestTestGen with DeepREST strategy..."
java $JAVA_OPTS -jar rtg.jar -s DeepRestStrategy &
JAVA_PID=$!

# Wait ONLY for Java to exit, then cleanup will kill Python
wait $JAVA_PID