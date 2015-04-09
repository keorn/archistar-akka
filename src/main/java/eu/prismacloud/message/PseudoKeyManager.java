package eu.prismacloud.message;

import org.bouncycastle.crypto.generators.Poly1305KeyGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 *
 * @author andy
 */
public class PseudoKeyManager {
    
    private final static KeyParameter key;
    
    static {
       byte[] tmp = new byte[32];
       Poly1305KeyGenerator.clamp(tmp);
       key = new KeyParameter(tmp);
    }

    public static KeyParameter keyFromTo(String sender, String recipient) {
        return key;
    }
}
