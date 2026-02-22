#!/bin/bash
# pa1_automation.sh

SERVER_IP="csa1.bu.edu"
PORT="58005"

echo "RTT Measurements:"
for size in 1 100 200 400 800 1000; do
    echo "Testing RTT: $size bytes..."
    python3 /home/ugrad/omkhadka/om-khadka-cs455/pa1/pa1part2/client.py $SERVER_IP $PORT rtt 10 $size 0.5
    sleep 1
done

echo -e "\nThroughput Measurements:"
for size in 1024 2048 4096 8192 16384 32768; do
    echo "Testing Throughput: $size bytes..."
    python3 /home/ugrad/omkhadka/om-khadka-cs455/pa1/pa1part2/client.py $SERVER_IP $PORT tput 10 $size 0.5
    sleep 1
done

# echo -e "\nServer Delay Tests:"
# for delay in 0 0.1 0.5; do
#     echo "Testing Delay: $delay sec..."
#     python3 /home/ugrad/omkhadka/om-khadka-cs455/pa1/pa1part2/client.py $SERVER_IP $PORT rtt 10 100 $delay
# done

echo -e "\nDone"