package net.tomp2p;

import java.io.IOException;

import net.tomp2p.connection.Bindings;
import net.tomp2p.connection.ChannelServerConfiguration;
import net.tomp2p.connection.Ports;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerMap;
import net.tomp2p.peers.PeerMapConfiguration;
import net.tomp2p.utils.InteropRandom;

public class BenchmarkUtil {

	/**
	 * Creates peers for benchmarking. The first peer will be used as the master.
     * This means that shutting down the master will shut down all other peers as well.
	 * @param nrOfPeers Number of peers to create.
	 * @param rnd The random object used for peer ID creation.
	 * @param port The UDP and TCP port.
	 * @param maintenance Indicates whether maintenance should be enabled.
	 * @param timeout Indicates whether timeout should be enabled.
	 * @return
	 * @throws IOException 
	 */
	public static Peer[] createNodes(int nrOfPeers, InteropRandom rnd, int port, boolean maintenance, boolean timeout) throws IOException
    {
        Peer[] peers = new Peer[nrOfPeers];

        Number160 masterId = createRandomId(rnd);
        PeerMap masterMap = new PeerMap(new PeerMapConfiguration(masterId));
        PeerBuilder pb = new PeerBuilder(masterId)
            .ports(port)
            .enableMaintenance(maintenance)
            .externalBindings(new Bindings())
            .peerMap(masterMap);
        if (!timeout) {
        	pb.channelServerConfiguration(createInfiniteTimeoutChannelServerConfiguration(port));
        }
        peers[0] = pb.start();
        System.out.printf("Created master peer: %s.\n", peers[0].peerID());

        for (int i = 1; i < nrOfPeers; i++)
        {
            peers[i] = createSlave(peers[0], rnd, maintenance, timeout);
        }
        return peers;
    }

    public static Peer createSlave(Peer master, InteropRandom rnd, boolean maintenance, boolean timeout) throws IOException
    {
        Number160 slaveId = createRandomId(rnd);
        PeerMap slaveMap = new PeerMap(new PeerMapConfiguration(slaveId).peerNoVerification());
        PeerBuilder pb = new PeerBuilder(slaveId)
            .masterPeer(master)
            .enableMaintenance(maintenance)
            .externalBindings(new Bindings())
            .peerMap(slaveMap);
        if (!timeout) {
        	pb.channelServerConfiguration(createInfiniteTimeoutChannelServerConfiguration(Ports.DEFAULT_PORT));
        }
        Peer slave = pb.start();
        System.out.printf("Created slave peer %s.\n", slave.peerID());
        return slave;
    }
    
    /**
     * Creates and returns a ChannelServerConfiguration that has infinite values for all timeouts.
     * @param port
     * @return
     */
    public static ChannelServerConfiguration createInfiniteTimeoutChannelServerConfiguration(int port)
    {
        return PeerBuilder.createDefaultChannelServerConfiguration()
            .idleTCPSeconds(0)
            .idleUDPSeconds(0)
            .connectionTimeoutTCPMillis(0)
            .ports(new Ports(port, port));
    }

    public static long startBenchmark(String caller)
    {
    	attemptGarbageCollection();
    	System.out.printf("%s: Starting Benchmarking...\n", caller);
        return System.nanoTime();
    }

    public static void stopBenchmark(long start, String caller)
    {
    	long stop = System.nanoTime();
    	long nanos = stop - start;
        System.out.printf("%s: Stopped Benchmarking.\n", caller);
        System.out.printf("%s: %s ns | %s ms | %s s\n", caller, toNanos(nanos), toMillis(nanos), toSeconds(nanos));
    }

    public static void attemptGarbageCollection()
    {
    	System.out.println("Garbage Collection attempted...");
    	Runtime.getRuntime().gc();
    }
    
    private static double toSeconds(long nanos)
    {
        return (double) nanos / 1000000000;
    }

    private static double toMillis(long nanos)
    {
    	return (double) nanos / 1000000;
    }

    private static double toNanos(long nanos)
    {
    	return nanos;
    }
    
    private static Number160 createRandomId(InteropRandom rnd) {
    	int[] vals = new int[Number160.INT_ARRAY_SIZE];
    	for (int i = 0; i < vals.length; i++) {
    		vals[i] = rnd.nextInt(Integer.MAX_VALUE);
    	}
    	return new Number160(vals);
    }
}
