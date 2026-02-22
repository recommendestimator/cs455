# INFO
    Om Khadka
    U51801771

# EXECUTING FILES
    The server file is all ready to go prepackaged. The client file, however, does do a bit more.

    1. In a terminal, execute the server.
        a. python3 server.py <port>
            <port> = the port you wanna use (58000 - 58999)

    2. In a seperate terminal (or device), then run the client.
        a. python3 client.py <hostname> <port> <type> <probes> <size> [delay]
            <hostname> = the target IP of the server. Set it to the IP/name of where the server is currently listening on from (1)
                OR localhost if you're running in locally
            
            <port> = the target port of the server. Like again, set it to the port you've previously chosen from (1)
            
            <type> = this determines the type of test being run. The following tests include:
                rtt = measures round trip time (latency)
                tput = measure throughput of (bandwidth)
            
            <probes> = # of msgs to send, in order to calculate an avg.
            
            <size> = size of the payload (in BYTES) per msgs
            
            [delay] = an OPTIONAL VAR, indicating how long (in seconds) you want the server to wait before echoing back a response

    3. Changing the msg in client.py is a bit tricker. Look at line 63:
            payload = 'A' * msg_size
        The msg is dependent on one of your vars you'll enter to initiate the client (SIZE), so be wary of that.

    4. Once all is set and done, you should get an output as such:
        a. SERVER SIDE
            Connection from ('128.197.11.40', 40350)
            CSP Received: s rtt 10 1 0.0
            Setup: type=rtt, probes=10, size=1, delay=0.0
            MP Received: m 1 A...
            MP Received: m 2 A...
            MP Received: m 3 A...
            MP Received: m 4 A...
            MP Received: m 5 A...
            MP Received: m 6 A...
            MP Received: m 7 A...
            MP Received: m 8 A...
            MP Received: m 9 A...
            MP Received: m 10 A...
            CTP Received: t
            Connection terminated
            Connection closed

        b. CLIENT SIDE (RTT)
            RTT Measurements:
            Testing RTT: 1 bytes...
            Connected to 10.239.123.179:58005
            CSP Sent: s rtt 10 1 0.0
            CSP Response: 200 OK: Ready

            Starting RTT measurements...
            Probes: 10, Payload Size: 1 bytes
            Probe 1: RTT = 1.61 ms
            Probe 2: RTT = 1.21 ms
            Probe 3: RTT = 1.39 ms
            Probe 4: RTT = 1.46 ms
            Probe 5: RTT = 1.73 ms
            Probe 6: RTT = 1.46 ms
            Probe 7: RTT = 1.69 ms
            Probe 8: RTT = 2.10 ms
            Probe 9: RTT = 1.41 ms
            Probe 10: RTT = 1.72 ms

            CTP Response: 200 OK: Closing Connection

            ==================================================
            RESULTS
            ==================================================
            Avg RTT: 1.58 ms
            Min RTT: 1.21 ms
            Max RTT: 2.10 ms
            Saved to rtt_data_1bytes.csv
            Connection closed

        c. CLIENT SIDE (TPUT)
            Throughput Measurements:
            Testing Throughput: 1024 bytes...
            Connected to 10.239.123.179:58005
            CSP Sent: s tput 10 1024 0.0
            CSP Response: 200 OK: Ready

            Starting TPUT measurements...
            Probes: 10, Payload Size: 1024 bytes
            Probe 1: RTT = 1.94 ms
            Probe 2: RTT = 1.61 ms
            Probe 3: RTT = 2.15 ms
            Probe 4: RTT = 1.57 ms
            Probe 5: RTT = 2.11 ms
            Probe 6: RTT = 1.68 ms
            Probe 7: RTT = 1.57 ms
            Probe 8: RTT = 1.89 ms
            Probe 9: RTT = 1.58 ms
            Probe 10: RTT = 1.57 ms

            CTP Response: 200 OK: Closing Connection

            ==================================================
            RESULTS
            ==================================================
            Total Time: 0.0179 sec
            Total Bytes: 10291 bytes
            Throughput: 4606.02 kbps (4.6060 Mbps)
            Saved to tput_data_1024bytes.csv
            Connection closed

    5. Finally, inside the folder where you've executed client.py, you should see a .csv file as such:
            {TYPE}_data_{SIZE}_delay{DELAY}s.csv
        This is for the purposes of graphing. You can graph your own charts too.