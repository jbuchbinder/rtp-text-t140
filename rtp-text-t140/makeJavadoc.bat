@echo off
echo Creating Javadoc for RTP text/t140 library ..
mkdir javadoc
javadoc -d .\javadoc -doctitle "RTP text/t140 library" -doctitle "RTP text/t140 library" -header "RTP text/t140 library" -breakiterator -classpath src -overview .\javadoc-overview.html se.omnitor.media.protocol.text.t140 se.omnitor.media.content.text.t140 se.omnitor.protocol.rtp.text
@pause


