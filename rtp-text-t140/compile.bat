@echo off
echo Compiling RTP text/t140 library ..
javac -classpath src src\se\omnitor\media\content\text\t140\*.java
javac -classpath src src\se\omnitor\media\protocol\text\t140\*.java
javac -classpath src src\se\omnitor\protocol\rtp\text\*.java
javac -classpath src src\se\omnitor\util\*.java
echo Compiling JMF example files ..
javac -classpath src src\example\jmf\*.java

@pause


