"""Exercise production ResDrawable without initializing game/cache/render systems."""
import pathlib
import subprocess

root = pathlib.Path(__file__).resolve().parents[1]
output = root / "build" / "drawable-fallback-tests"
output.mkdir(parents=True, exist_ok=True)
sources = [root / "src/haven/ResDrawable.java"] + sorted((root / "tests/drawable").rglob("*.java"))
subprocess.run(["javac", "--release", "8", "-encoding", "UTF-8", "-d", str(output)] +
               [str(p) for p in sources], check=True)
subprocess.run(["java", "-cp", str(output), "haven.DrawableFallbackTest"], check=True)
subprocess.run(["java", "-Dkami.test.missing-world-resource=gfx/terobjs/cupboard",
                "-cp", str(output), "haven.DrawableFallbackTest"], check=True)
