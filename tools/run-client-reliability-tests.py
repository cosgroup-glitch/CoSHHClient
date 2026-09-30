"""Compile isolated production code; never initialize game settings or caches."""
import pathlib
import subprocess

root = pathlib.Path(__file__).resolve().parents[1]
output = root / "build" / "client-reliability-tests"
output.mkdir(parents=True, exist_ok=True)
sources = [root / "src/haven/ClientIntegrity.java", root / "src/haven/Finalizer.java",
           root / "tests/client/FinalizerFixtures.java", root / "tests/client/ClientReliabilityTest.java"]
subprocess.run(["javac", "--release", "8", "-encoding", "UTF-8", "-d", str(output)] +
               [str(p) for p in sources], check=True)
subprocess.run(["java", "-cp", str(output), "haven.ClientReliabilityTest", str(output)], check=True)
