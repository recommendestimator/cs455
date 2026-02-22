import matplotlib.pyplot as plt
import csv
import glob
import os

# --- Graph 1: RTT vs. Message Size ---
rtt_sizes = [1, 100, 200, 400, 800, 1000]
rtt_avg = []

for size in rtt_sizes:
    filename = f"rtt_data_{size}bytes_delay0.5s.csv"
    # Fallback if delay isn't in filename
    if not os.path.exists(filename):
        filename = f"rtt_data_{size}bytes.csv"
    
    try:
        with open(filename, 'r') as f:
            reader = csv.DictReader(f)
            rtts = [float(row['rtt_ms']) for row in reader]
            rtt_avg.append(sum(rtts) / len(rtts))
    except FileNotFoundError:
        print(f"Warning: Missing {filename}")
        rtt_avg.append(0)

plt.figure(figsize=(10, 5))
plt.plot(rtt_sizes, rtt_avg, 'bo-', label='RTT')
plt.xlabel('Message Size (bytes)')
plt.ylabel('Average RTT (ms)')
plt.title('TCP Round Trip Time vs. Message Size (Server Delay = 0.5)')
plt.grid(True)
plt.savefig('graph_rtt.png')
plt.show()

# --- Graph 2: Throughput vs. Message Size ---
tput_sizes = [1024, 2048, 4096, 8192, 16384, 32768]
tput_avg = []

for size in tput_sizes:
    filename = f"tput_data_{size}bytes.csv"
    
    try:
        with open(filename, 'r') as f:
            reader = csv.DictReader(f)
            row = next(reader)
            tput_avg.append(float(row['throughput_kbps']))
    except FileNotFoundError:
        print(f"Warning: Missing {filename}")
        tput_avg.append(0)

plt.figure(figsize=(10, 5))
plt.plot(tput_sizes, tput_avg, 'rs-', label='Throughput')
plt.xlabel('Message Size (bytes)')
plt.ylabel('Throughput (kbps)')
plt.title('TCP Throughput vs. Message Size (Server Delay = 0.5)')
plt.grid(True)
plt.savefig('graph_throughput.png')
plt.show()