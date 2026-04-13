public class Packet
{
    private int seqnum;
    private int acknum;
    private int checksum;
    private String payload;
    public int[] sack;
    
    public Packet(Packet p)
    {
        seqnum = p.getSeqnum();
        acknum = p.getAcknum();
        checksum = p.getChecksum();
        payload = new String(p.getPayload());

        // So that SACK array doesn't die upon transmission.
        if (p.sack != null) {
            this.sack = p.sack.clone();
        } else {
            this.sack = new int[5];
        }

    }
    
    public Packet(int seq, int ack, int check, String newPayload)
    {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        this.sack = new int[5];

        if (newPayload == null)
        {
            payload = "";
        }        
        else if (newPayload.length() > NetworkSimulator.MAXDATASIZE)
        {
            payload = null;
        }
        else
        {
            payload = new String(newPayload);
        }
    }
    
    public Packet(int seq, int ack, int check)
    {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        payload = "";
        this.sack = new int[5];
    }    
        

    public boolean setSeqnum(int n)
    {
        seqnum = n;
        return true;
    }
    
    public boolean setAcknum(int n)
    {
        acknum = n;
        return true;
    }
    
    public boolean setChecksum(int n)
    {
        checksum = n;
        return true;
    }
    
    public boolean setPayload(String newPayload)
    {
        if (newPayload == null)
        {
            payload = "";
            return false;
        }        
        else if (newPayload.length() > NetworkSimulator.MAXDATASIZE)
        {
            payload = "";
            return false;
        }
        else
        {
            payload = new String(newPayload);
            return true;
        }
    }
    
    public int getSeqnum()
    {
        return seqnum;
    }
    
    public int getAcknum()
    {
        return acknum;
    }
    
    public int getChecksum()
    {
        return checksum;
    }
    
    public String getPayload()
    {
        return payload;
    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("seqnum: ").append(seqnum)
        .append(" acknum: ").append(acknum)
        .append(" checksum: ").append(checksum)
        .append(" payload: ").append(payload)

        .append(" sack: [");
        for (int i = 0; i < 5; i++) {
            sb.append(sack[i]).append(i < 4 ? "," : "");
        }
        sb.append("]");

        return sb.toString();
    }
    
}