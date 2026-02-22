#!/usr/bin/env python3
import sys
import socket
import time

def main():
    # Make sure we got right args
    # Note, delay is optional
    if len(sys.argv) < 6:
        print("Usage: python3 client.py <hostname> <port> <type> <probes> <size> [delay]")
        print("type: 'rtt' or 'tput'")
        print("probes: number of probe messages (e.g., 10)")
        print("size: payload size in bytes")
        print("delay: server delay in seconds (default: 0)")
        sys.exit(1)
    
    hostname = sys.argv[1]
    port = int(sys.argv[2])
    meas_type = sys.argv[3]
    num_probes = int(sys.argv[4])
    msg_size = int(sys.argv[5])
    
    # If delay in args, set that too
    server_delay = 0.0
    if len(sys.argv) > 6:
        server_delay = float(sys.argv[6])
    
    # Create TCP socket
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # Timeout just incase
    client_socket.settimeout(10.0)
    
    try:
        # We're trying to connect to server now.
        client_socket.connect((hostname, port))
        print(f"Connected to {hostname}:{port}")
        
        # CSP
        csp_msg = f"s {meas_type} {num_probes} {msg_size} {server_delay}\n"
        client_socket.sendall(csp_msg.encode())
        print(f"CSP Sent: {csp_msg.strip()}")
        
        csp_response = client_socket.recv(1024).decode().strip()
        print(f"CSP Response: {csp_response}")
        
        if "404" in csp_response:
            print("Server rejected the connection setup")
            sys.exit(1)
        
        # MP
        print(f"\nStarting {meas_type.upper()} measurements...")
        print(f"Probes: {num_probes}, Payload Size: {msg_size} bytes")
        
        rtt_times = []
        total_bytes = 0
        start_time = time.time()
        
        # num_probes = # of times we check connection
        # We essentially run this num_probes time, getting the RTT, saving it, and then logging it to console.
        # Also, the msg for now will just "A" * msg_size (msg_size number of "A"'s)
        for seq in range(1, num_probes + 1):
            payload = 'A' * msg_size
            mp_msg = f"m {seq} {payload}\n"
            
            send_time = time.time()
            
            client_socket.sendall(mp_msg.encode())
            
            response = client_socket.recv(65536)
            recv_time = time.time()
            
            # Calculate RTT for this probe
            rtt = (recv_time - send_time) * 1000  # ms
            rtt_times.append(rtt)
            total_bytes += len(mp_msg.encode())
            
            print(f"Probe {seq}: RTT = {rtt:.2f} ms")
        
        end_time = time.time()
        total_time = end_time - start_time
        
        # CTP
        client_socket.sendall("t\n".encode())
        ctp_response = client_socket.recv(1024).decode().strip()
        print(f"\nCTP Response: {ctp_response}")
        
        # Calculating & Display results
        print("\n" + "="*50)
        print("RESULTS")
        print("="*50)
        
        # Saving data as an RTT test
        if meas_type == 'rtt':
            avg_rtt = sum(rtt_times) / len(rtt_times)
            min_rtt = min(rtt_times)
            max_rtt = max(rtt_times)
            print(f"Avg RTT: {avg_rtt:.2f} ms")
            print(f"Min RTT: {min_rtt:.2f} ms")
            print(f"Max RTT: {max_rtt:.2f} ms")
            
            # Save as a CSV
            with open(f"rtt_data_{msg_size}bytes_delay{server_delay}s.csv", "w") as f:
                f.write("probe,rtt_ms\n")
                for i, rtt in enumerate(rtt_times, 1):
                    f.write(f"{i},{rtt:.2f}\n")
            print(f"Saved to rtt_data_{msg_size}bytes.csv")
        
        # Saving data as an TPUT test
        elif meas_type == 'tput':
            # TP = (total_bytes * 8) / total_time  (in b/s)
            throughput_bps = (total_bytes * 8) / total_time
            throughput_kbps = throughput_bps / 1000
            throughput_mbps = throughput_bps / 1000000
            
            print(f"Total Time: {total_time:.4f} sec")
            print(f"Total Bytes: {total_bytes} bytes")
            print(f"Throughput: {throughput_kbps:.2f} kbps ({throughput_mbps:.4f} Mbps)")
            
            # Save as a CSV
            with open(f"tput_data_{msg_size}bytes.csv", "w") as f:
                f.write("total_bytes,total_time,throughput_kbps\n")
                f.write(f"{total_bytes},{total_time:.4f},{throughput_kbps:.2f}\n")
            print(f"Saved to tput_data_{msg_size}bytes.csv")
        
    # Timeout
    except socket.timeout:
        print("Error: Connection timed out")
        sys.exit(1)
    # Server isn't open
    except ConnectionRefusedError:
        print("Error: Connection refused (server not running)")
        sys.exit(1)
    # Any other err's
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)
    # Make sure to always close out the process.
    finally:
        client_socket.close()
        print("Connection closed")

if __name__ == "__main__":
    main()