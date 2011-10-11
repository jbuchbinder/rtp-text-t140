@echo off
echo Compiling RTP text/t140 library ..
javac se\omnitor\media\content\text\t140\*.java
javac se\omnitor\media\protocol\text\t140\*.java
javac se\omnitor\protocol\rtp\text\*.java
javac se\omnitor\util\*.java
echo Compiling JMF example files ..
javac example\jmf\*.java

@pause


