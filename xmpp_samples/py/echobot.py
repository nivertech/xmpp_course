#! /usr/bin/python

# XMPP echo bot
# REQUIRED:
#  sudo pip install sleekxmpp

import sleekxmpp
import traceback # for debug purposes

JID = "echobot@xmpp.nivertech.com/EchoBot"
PWD = "mypass"

class   EchoBot:

    def __init__(self, jid, password):
        self.xmpp = sleekxmpp.ClientXMPP(jid, password)
        self.xmpp.add_event_handler("session_start", self.handleXMPPConnected)
        self.xmpp.add_event_handler("message",       self.handleIncomingMessage)

    def run(self):
        self.xmpp.connect()
        print "connected\n"
        self.xmpp.process(threaded=False)

    def handleXMPPConnected(self, event):
        print "handleIncomingMessage:\n%s\n" % str(event)
        self.xmpp.sendPresence(pstatus="Send me a message")

    def handleIncomingMessage(self, message):
        print "handleIncomingMessage:\n%s\n" % str(message)
        try:
            reply_message = message.reply(message['body'])
            reply_message['from'] = JID
            reply_message.send()
        except:
            print traceback.format_exc()
            
def main():
    # TODO : replace JID and password
    bot = EchoBot(JID, PWD)
    bot.run()

if __name__ == '__main__':
    main()
