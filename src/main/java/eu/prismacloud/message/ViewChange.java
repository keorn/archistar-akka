package eu.prismacloud.message;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author ait
 */
public class ViewChange {
    
    class Certificate {
        public final int n;
        public final int d;
        public final int v;
        
        public Certificate(int n, int d, int v) {
            this.n = n;
            this.d = d;
            this.v = v;
        }
    }
    
    ViewChange(int replicaId, int viewNr, int h, Map<Integer, Byte[]> c, Set<Certificate> q, Set<Certificate> p, byte[] mac) {
        this.replicaId = replicaId;
        this.viewNr = viewNr;
        this.h = h;
        this.c = c;
        this.q = q;
        this.p = p;
        this.mac = mac;
    }
    
    /** the new view */
    public final int viewNr;
    
    /** sequence number of the last known stable checkpoint */
    public final int h;
    
    /** [sequencenr, digest-of-checkpoints] */
    public final Map<Integer, Byte[]> c;
    
    public final int replicaId;
    
    public final byte[] mac;
    
    /** pre-prepared */
    public final Set<Certificate> q;
    
    /** prepared */
    public final Set<Certificate> p;
}
