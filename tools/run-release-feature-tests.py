"""Run packaged release feature logic with new fixture-only settings and cache roots."""
import os
import pathlib
import subprocess
import tempfile
import uuid

root = pathlib.Path(__file__).resolve().parents[1]
output = root / "build" / "release-feature-tests"
output.mkdir(parents=True, exist_ok=True)
fixture = pathlib.Path(tempfile.mkdtemp(prefix="run-", dir=output))
for name in ("classes", "settings", "appdata", "home"):
    (fixture / name).mkdir()
subprocess.run(["javac", "--release", "8", "-encoding", "UTF-8", "-d", str(fixture / "classes"),
                "-cp", str(root / "build/hafen.jar"), str(root / "tests/client/ReleaseFeaturesTest.java")], check=True)
classpath = os.pathsep.join(str(p) for p in [fixture / "classes", root / "build/hafen.jar",
                             root / "lib/ext/jogl/*", root / "lib/ext/lwjgl/*"])
env = os.environ.copy()
env["APPDATA"] = str(fixture / "appdata")
subprocess.run(["java", "-Djava.awt.headless=true", "-Dhaven.uiscale=1.0",
               "-Dconfig.homedir=workdir", "-Duser.home=" + str(fixture / "home"),
               "-Dhaven.prefspec=ReleaseFeatureFixture-" + uuid.uuid4().hex,
               "-Dkami.update.manifest=", "-cp", classpath, "haven.ReleaseFeaturesTest", str(fixture)],
               cwd=fixture / "settings", env=env, check=True)
