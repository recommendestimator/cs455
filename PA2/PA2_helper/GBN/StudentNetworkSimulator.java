import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator
{
    //
    // FIELDS
    //

    // DONT TOUCH
    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo; 

    // Entity A
    private int base;                  // Oldest unACKed seq.#
    private int nextseqnum;            // Next seq.# to send
    private boolean timerRunning = false;
    private Packet[] snd_buf;          // Sender window buffer
    private ArrayList<Message> pendingMessages = new ArrayList<>();
    
    // Entity B
    private int expectedseqnum;             // Next expected in-order seq.#
    private int[] sackHistory = new int[5]; // Tracks the last 5 received seq.# (for SACK)
    private int sackIdx = 0;                // Circular index for sackHistory
    
    // Statistical (Debugging)
    private int numOriginalPackets;
    private int numRetransmissions;
    private int numDeliveredToLayer5;
    private int numACKsSent;
    private int numCorruptedPackets;
    private int totalPacketsSent;

    // RTT, Comm. Time (Debugging)
    private double[] sendTime;         // Time when packet was first sent
    private boolean[] hasRetransmitted;// Whether packet has been retransmitted
    private double totalRTT = 0.0;
    private int rttCount = 0;
    private double totalCommTime = 0.0;
    private int commTimeCount = 0;


    //
    // CONSTRUCTOR(S)
    //

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize*2; // Set to SR
        RxmtInterval = delay;

        // Init. the debug vars
        numOriginalPackets = 0;
        numRetransmissions = 0;
        numDeliveredToLayer5 = 0;
        numACKsSent = 0;
        numCorruptedPackets = 0;
        totalPacketsSent = 0;
        sendTime = new double[LimitSeqNo];
        hasRetransmitted = new boolean[LimitSeqNo];

        // GBN vars
        snd_buf = new Packet[WindowSize];
        Arrays.fill(sackHistory, -1);
    }

    //
    // METHODS
    //

    // Checksum calc.
    private int calculateChecksum(Packet packet) {
        int sum = 0;
        sum += packet.getSeqnum();
        sum += packet.getAcknum();
        
        // For GBN, include SACK block too
        if (packet.sack != null) {
            for (int i = 0; i < 5; i++) {
                sum += packet.sack[i];
            }
        }
        
        String payload = packet.getPayload();
        // Calc. payload (if any)
        if (payload != null) {
            for (int i = 0; i < payload.length(); i++) {
                sum += (int) payload.charAt(i);
            }
        }

        return sum;
    }
    
    // Corruption check (just checks sent checksum to packet checksum)
    private boolean isCorrupted(Packet packet) {
        boolean corrupted = (calculateChecksum(packet) != packet.getChecksum());

        // Debug
        if (corrupted) {
            System.out.println("[DEBUG] DROPPED PACKET: seq=" + packet.getSeqnum() + 
                            " ack=" + packet.getAcknum() + " entity=" + (packet.getSeqnum() == -1 ? "B" : "A"));
        }

        return corrupted;
    }


    // A wants to send a msg (layer 5)
    protected void aOutput(Message message)
    {
        // Add msg to queue, and just send AMAP (as much as possible)
        pendingMessages.add(message);
        sendPackets();
    }

    // Helper for aOutput: Handles logic when the window isn't full (& there's pending msgs)
    private void sendPackets() {
        while (!pendingMessages.isEmpty() && (nextseqnum < base + WindowSize)) {
            Message msg = pendingMessages.remove(0);
            
            // Packet has seq# = MOD limitSeq#
            int seq = nextseqnum % LimitSeqNo;
            Packet pkt = new Packet(seq, -1, 0, msg.getData());
            pkt.setChecksum(calculateChecksum(pkt));
            
            /* Then do the following:
                - Store in snd win buf (MOD WindowSize as index)
                - Send to layer3
                - Update debug
                    - Stats
                    - Tracing (RTT, Comm.)
                - Start the timer:
                    - ONLY if the current packet is the FIRST packet being sent (as in, it's base).
                    - If we're sending a prev. packet, its timer would alr be running.
                - Update next seq# num
            */
            snd_buf[nextseqnum % WindowSize] = pkt;
            
            toLayer3(A, pkt);
            
            numOriginalPackets++;
            totalPacketsSent++;

            if (sendTime[seq] == 0.0) {
                sendTime[seq] = getTime();
                hasRetransmitted[seq] = false;
            }
            
            if (nextseqnum == base) { // Only for base pkt
                startTimer(A, RxmtInterval);
                timerRunning = true;
            }

            nextseqnum++;
        }
    }
    
    // A gets a packet (its an ACK, based on assignment details)
    protected void aInput(Packet packet)
    {
        if (isCorrupted(packet)) {
            numCorruptedPackets++;
            return;
        }

        int ack = packet.getAcknum();
        int baseMod = base % LimitSeqNo;
        int diff = (ack - baseMod + LimitSeqNo) % LimitSeqNo; // dist from base
        
        // Only process ACKs that advance the window
        if (diff > 0 && diff < WindowSize) {
            int logicalAcked = base + diff;
            
            // Update stats for newly ACKed packets
            for (int i = base; i <= logicalAcked; i++) {
                int idx = i % LimitSeqNo;
                
                if (sendTime[idx] != 0.0) {
                    double commTime = getTime() - sendTime[idx];
                    totalCommTime += commTime;
                    commTimeCount++;
                    
                    if (!hasRetransmitted[idx]) {
                        totalRTT += commTime;
                        rttCount++;
                    }
                    sendTime[idx] = 0.0;
                }
            }

            // Slide window
            int oldBase = base;
            base = logicalAcked + 1;

                                
            // If the window is now empty, stop timer.
            if (base == nextseqnum) {
                stopTimer(A);
                timerRunning = false;
            } 
            // Else, still got unACKed pkts --> Stop the old timer (for old base) and start new timer (for new base).
            else {
                stopTimer(A);
                startTimer(A, RxmtInterval);
                timerRunning = true;
            }
            
            sendPackets(); // Slide window
        }

        // Ignore dup ACKs (for GBN) 
    }
    
    // A's packet expired
    // Retransmit ALL unACKed packets
    protected void aTimerInterrupt()
    {
        // For trace == 3
        System.out.println("TRACE_CASE_3: Timeout. Retransmitting packet " + base);
        
        int seq = base;
        while (seq != nextseqnum) {
            Packet p = snd_buf[seq % WindowSize];
            if (p != null) {
                toLayer3(A, p);
                numRetransmissions++;
                totalPacketsSent++;
                hasRetransmitted[seq % LimitSeqNo] = true;
            }
            seq = (seq + 1) % LimitSeqNo;
        }
        
        stopTimer(A);
        startTimer(A, RxmtInterval);
        timerRunning = true;
    }
    
    // A (sender) init
    protected void aInit()
    {
        base = 0;
        nextseqnum = 0;
        timerRunning = false;
        pendingMessages.clear();
    }
    
    // B gets a packet
    protected void bInput(Packet packet)
    {
        // Pkt corruption check. Also send dup ACK for expected
        if (isCorrupted(packet)) {
            numCorruptedPackets++;
            sendACK(expectedseqnum - 1);
            return;
        }

        int seq = packet.getSeqnum();
        recordInSACK(seq); // Always record for SACK block

        // An in-order pkt
        if (seq == expectedseqnum % LimitSeqNo) {
            toLayer5(packet.getPayload());
            numDeliveredToLayer5++;
            expectedseqnum++;
            sendACK(expectedseqnum - 1);
        } 
        // out of order OR dup --> discard payload
        else {
            if (traceLevel >= 3 && seq != expectedseqnum % LimitSeqNo) {
                System.out.println("TRACE_CASE_4: OOO/Dup packet " + seq + ". Discarding. Sending dup ACK " + (expectedseqnum - 1));
            }

            sendACK(expectedseqnum - 1);
        }
    }

    // Helper method for bInput(): Sends ACK
    private void sendACK(int ackNum) {
        int ackToSend = ackNum % LimitSeqNo;
        if (ackToSend < 0) ackToSend += LimitSeqNo;
        
        Packet ackPacket = new Packet(-1, ackToSend, 0, " ");
        
        // Fill SACK block with last 5 received seqnums
        for (int i = 0; i < 5; i++) {
            int val = sackHistory[(sackIdx + i) % 5];
            ackPacket.sack[i] = (val == -1) ? 0 : val;
        }
        
        ackPacket.setChecksum(calculateChecksum(ackPacket));
        toLayer3(B, ackPacket);
        numACKsSent++;
    }

    // Helper method for bInput(): Records sackID in sack History
    private void recordInSACK(int seq) {
        sackHistory[sackIdx] = seq;
        sackIdx = (sackIdx + 1) % 5;
    }


    // B (reciever) init
    protected void bInit()
    {
        expectedseqnum = 0;
        Arrays.fill(sackHistory, -1);
        sackIdx = 0;
    }

    // Debug/Stats
    protected void Simulation_done()
    {
        System.out.println("\n\n===============STATISTICS=======================");
        System.out.println("Number of original packets transmitted by A: " + numOriginalPackets);
        System.out.println("Number of retransmissions by A: " + numRetransmissions);
        System.out.println("Number of data packets delivered to layer 5 at B: " + numDeliveredToLayer5);
        System.out.println("Number of ACK packets sent by B: " + numACKsSent);
        System.out.println("Number of corrupted packets: " + numCorruptedPackets);
        
        int totalSent = numOriginalPackets + numRetransmissions + numACKsSent;
        double lostRatio = 0.0;
        if (totalSent > 0) {
            lostRatio = (double)(numRetransmissions - numCorruptedPackets) / totalSent;
            if (lostRatio < 0) lostRatio = 0.0;
        }
        
        double corruptRatio = 0.0;
        double denom = totalSent - (numRetransmissions - numCorruptedPackets);
        if (denom > 0) {
            corruptRatio = (double)numCorruptedPackets / denom;
        }
        
        System.out.println("Ratio of lost packets: " + lostRatio);
        System.out.println("Ratio of corrupted packets: " + corruptRatio);
        
        double avgRTT = rttCount > 0 ? totalRTT / rttCount : 0.0;
        double avgCommTime = commTimeCount > 0 ? totalCommTime / commTimeCount : 0.0;
        
        System.out.println("Average RTT: " + avgRTT);
        System.out.println("Average communication time: " + avgCommTime);
        System.out.println("==================================================");
    }	
}