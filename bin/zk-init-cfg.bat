@echo off
setlocal

set RUNDIR=%~dp0
set MAINCLASS=com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigInit

@echo off
tnt4j-streams.bat -c -f:%RUNDIR%..\config\zk-init-cfg.properties