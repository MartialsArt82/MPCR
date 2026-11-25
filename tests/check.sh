#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME to a JDK}"
test_dir=$(mktemp -d)
server_pid=
cleanup() {
    if [[ -n "$server_pid" ]]; then kill "$server_pid" 2>/dev/null || true; fi
    rm -rf "$test_dir"
}
trap cleanup EXIT
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d "$test_dir" common/src/mpc/remote/*.java tests/ProtocolCheck.java
for mode in normal empty reject bad-proof; do
    python3 tests/mock_mpc.py --mode "$mode" > "$test_dir/$mode.log" &
    server_pid=$!
    for _ in {1..50}; do
        [[ -s "$test_dir/$mode.log" ]] && break
        sleep 0.1
    done
    read -r port < "$test_dir/$mode.log"
    "$JAVA_HOME/bin/java" -cp "$test_dir" mpc.remote.ProtocolCheck "$port" "$mode"
    wait "$server_pid"
    server_pid=
done
