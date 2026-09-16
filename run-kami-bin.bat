@echo off
setlocal
cd /d "%~dp0bin"
java -Dsun.java2d.uiScale.enabled=false -Djava.net.preferIPv6Addresses=system -Dkami.modelscan=true --add-exports=java.base/java.lang=ALL-UNNAMED --add-exports=java.desktop/sun.awt=ALL-UNNAMED --add-exports=java.desktop/sun.java2d=ALL-UNNAMED -jar hafen.jar
endlocal
