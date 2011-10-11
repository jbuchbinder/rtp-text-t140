This is an example of how to use the RTP text/t140 library with JMF.

Compile all and run RtpChat:

  java RtpChat <ip address> <remote port> <local port> ["red"]


The RtpChat application communicates with another RtpChat running on another
computer. Run two RtpChats and let them point to each other.


Example:

Computer 1 (192.168.0.101):
- "java RtpChat 192.168.0.102 12000 13000 red"

Computer 2 (192.168.0.102):
- "java RtpChat 192.168.0.101 13000 12000 red"




