from custom_topologies import *
from test_custom_topo import Tester
import sys
import time

# Mininet libraries
from mininet.topo import Topo
from mininet.net  import Mininet
from mininet.node import RemoteController, OVSSwitch
from mininet.cli  import CLI

# Constants
CONTROLLER_IP = '127.0.0.1'  # String
CONTROLLER_PORT = 6633  # Integer

# Launch Network
def launch(custom_topo=LinearFive, test_args={'with_tests': False}):
    # passing Topology Class name as function argument
    topo = custom_topo()

    ctrl = RemoteController("c0", ip=CONTROLLER_IP, port=CONTROLLER_PORT)

    net = Mininet(
            topo          = topo,
            switch        = OVSSwitch,
            controller    = ctrl,
            autoSetMacs   = True,
            autoStaticArp = True,
            build         = True,
            cleanup       = True
    )
    net.start()

    if test_args['with_tests']:
        tester = Tester(net)
        # Give the network time to converge
        time.sleep(2)
        tester.test_source_ping(test_args['src_host'], test_args['duration'])
        # Give time to complete the test before network shutdown
        time.sleep(5)
    else:
        CLI( net )

    net.stop()

# Main
if __name__ == "__main__":
    test_args = {'with_tests': False, 'src_host':'', 'duration': 0}

    AVAILABLE_CUSTOM_TOPOLOGIES = {'LinearTwo': LinearTwo,
                                   'LinearFive': LinearFive,
                                   'TreeTwo': TreeTwo}

    # One input parameter; must be the Mininet custom topo.
    # Start without traffic tests.
    if len(sys.argv) == 2:
        test_mode = False
    #  input parameters; Mininet topo and traffic test toggle.
    # Starts traffic tests.
    elif len(sys.argv) == 5:
        # -t should be given to denote test mode
        if sys.argv[2] == "--with-tests":
            test_args['with_tests'] = True
            test_args['src_host'] = sys.argv[3]  # String
            test_args['duration'] = int(sys.argv[4])  # Integer
        # Unsupported command
        else:
            print "Expected test mode toggle --with-tests but you provided", sys.argv[2], "instead"
            sys.exit()
    # Unsupported extra parameters
    else:
        print "You have provided", len(sys.argv), " command-line arguments. That is incorrect."
        print "First mandatory argument is the custom topology name. Currently available:", AVAILABLE_CUSTOM_TOPOLOGIES.keys()
        print "If you wish test your topology, you can also provide optional parameters:", test_args.keys()
        sys.exit()

    # Start network
    custom_topo = AVAILABLE_CUSTOM_TOPOLOGIES[sys.argv[1]]
    launch(custom_topo, test_args)
