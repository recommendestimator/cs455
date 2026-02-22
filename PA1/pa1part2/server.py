import sys
import socket
import time

def main():
    # Make sure we using valid args
    if len(sys.argv) != 2:
        print("Using: python3 server.py <port>")
        sys.exit(1)
    
    port = int(sys.argv[1])
    
    # Validate port range
    if not (58000 <= port <= 58999):
        print("Warning: Not within port range of 58000-58999")
    
    # Make TCP socket, bind, then listen
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    server_socket.bind(('', port))
    server_socket.listen(1)
    print(f"Server listening on port {port}...")
    
    while True:
        # Client requesting, accept
        client_conn, client_addr = server_socket.accept()
        print(f"\nConnection from {client_addr}")
        
        try:
            # CSP setup
            csp_msg = client_conn.recv(1024).decode().strip()
            print(f"CSP Received: {csp_msg}")
            
            # CSP msg format should be
            # s <type> <probes> <size> <delay>
            parts = csp_msg.split()
            if len(parts) != 5 or parts[0] != 's':
                client_conn.sendall("404 ERROR: Incorrect Connection Setup Vars\n".encode())
                client_conn.close()
                continue
            
            # Turn the msg into its own vars.
            try:
                meas_type = parts[1]
                num_probes = int(parts[2])
                msg_size = int(parts[3])
                server_delay = float(parts[4])
            except ValueError:
                client_conn.sendall("404 ERROR: Incorrect Connection Setup Vars\n".encode())
                client_conn.close()
                continue
            
            # Also confirm the that <type> is valid.
            if meas_type not in ['rtt', 'tput']:
                client_conn.sendall("404 ERROR: Incorrect Connection Setup Vars\n".encode())
                client_conn.close()
                continue
            
            print(f"Setup: type={meas_type}, probes={num_probes}, size={msg_size}, delay={server_delay}")
            client_conn.sendall("200 OK: Ready\n".encode())
            
            # MP setup
            expected_seq = 1
            for i in range(num_probes):
                mp_msg = client_conn.recv(65536).decode().strip()
                print(f"MP Received: {mp_msg[:50]}...")  # just print 1st 50 chars
                
                # MP msg format
                # m <seq_num> <payload>
                mp_parts = mp_msg.split(' ', 2)  # 3 parts max
                if len(mp_parts) < 3 or mp_parts[0] != 'm':
                    client_conn.sendall("404 ERROR: Invalid Measurement Msg\n".encode())
                    client_conn.close()
                    break
                
                try:
                    seq_num = int(mp_parts[1])
                    payload = mp_parts[2]
                except ValueError:
                    client_conn.sendall("404 ERROR: Invalid Measurement Msg\n".encode())
                    client_conn.close()
                    break
                
                # Make sure seq. # is valid too.
                if seq_num != expected_seq:
                    client_conn.sendall("404 ERROR: Invalid Measurement Msg\n".encode())
                    client_conn.close()
                    break
                
                # Here we apply server delay (if any)
                if server_delay > 0:
                    time.sleep(server_delay)
                
                # Echo back the msg
                client_conn.sendall(mp_msg.encode())
                expected_seq += 1
            
            # CTP setup
            ctp_msg = client_conn.recv(1024).decode().strip()
            print(f"CTP Received: {ctp_msg}")
            
            if ctp_msg == 't':
                client_conn.sendall("200 OK: Closing Connection\n".encode())
                print("Connection terminated")
            else:
                client_conn.sendall("404 ERROR: Incorrect Connection Termination Msg\n".encode())
                print("Incorrect termination msg")
            
        # In case any other err's occur
        except Exception as e:
            print(f"Error: {e}")
            try:
                client_conn.sendall(f"404 ERROR: {str(e)}\n".encode())
            except:
                pass
        # Make sure we always close connection
        finally:
            client_conn.close()
            print("Connection closed\n")

if __name__ == "__main__":
    main()