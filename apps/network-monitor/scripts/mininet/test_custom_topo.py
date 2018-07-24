import sys
import time

# Mininet libraries
from mininet.net import Mininet

class Tester():
    def __init__(self, net):
        # Get an instance of the network
        self.net = net

    def test_source_ping(self, src_host, test_duration):
        # Ping all available hosts from src_host
        # src_host is a String

        end_time = time.time() + test_duration
        while (time.time() < end_time):

            output = src_host + ": "

            for dst_host in self.net.hosts:
                if dst_host.name == src_host:
                    # src_host should not ping itself
                    continue
                command = "ping -c 1 " + dst_host.IP()
                result = self.net.get(src_host).cmd(command)
                if "100% packet loss" in result:
                    output += " X    "
                else:
                    output += "%3s   " %dst_host.name

            # The line of stdout is replaced in every iteration
            sys.stdout.write("\r"+output)
            sys.stdout.flush()

            print ""
            time.sleep(1)
