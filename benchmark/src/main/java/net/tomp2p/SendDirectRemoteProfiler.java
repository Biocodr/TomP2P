package net.tomp2p;

import net.tomp2p.futures.FutureChannelCreator;
import net.tomp2p.futures.FutureResponse;
import net.tomp2p.p2p.builder.SendDirectBuilder;
import net.tomp2p.peers.PeerAddress;

public class SendDirectRemoteProfiler extends SendDirectProfiler {

	private static final int NETWORK_SIZE = 1;
	private PeerAddress remoteAddress;
	
	public SendDirectRemoteProfiler(boolean isForceUdp) {
		super(isForceUdp);
	}
	
	@Override
	protected void setup(Arguments args) throws Exception {

		Network = BenchmarkUtil.createNodes(NETWORK_SIZE, Rnd, 9099, false, false);
		sender = Network[0];
		remoteAddress = (PeerAddress) args.getParam();
		
		FutureChannelCreator fcc = sender.connectionBean().reservation().create(isForceUdp ? 1 : 0, isForceUdp ? 0 : 1);
		fcc.awaitUninterruptibly();
		cc = fcc.channelCreator();
		
		sendDirectBuilder = new SendDirectBuilder(sender, (PeerAddress) null)
			.idleUDPSeconds(0)
			.idleTCPSeconds(0)
			.buffer(createSampleBuffer())
			.forceUDP(isForceUdp);
	}
	
	@Override
	protected void execute() throws Exception {
		
		FutureResponse fr = sender.directDataRPC().send(remoteAddress, sendDirectBuilder, cc);
		fr.awaitUninterruptibly();
		
		// make buffer reusable
		sendDirectBuilder.buffer().reset();
		sendDirectBuilder.buffer().buffer().readerIndex(0);
	}
}
