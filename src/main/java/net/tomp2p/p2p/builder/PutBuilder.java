package net.tomp2p.p2p.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.tomp2p.futures.FutureChannelCreator;
import net.tomp2p.futures.FutureCreator;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

public class PutBuilder extends DHTBuilder<PutBuilder>
{
	private Entry<Number160, Data> data;
	private Map<Number160, Data> dataMap;
	//
	private boolean putIfAbsent = false;
	
	public PutBuilder(Peer peer, Number160 locationKey)
	{
		super(peer, locationKey);
		self(this);
	}
	
	public Entry<Number160, Data> getData()
	{
		return data;
	}

	public PutBuilder setData(final Number160 key, final Data data)
	{
		this.data = new Entry<Number160, Data>()
		{
			@Override
			public Data setValue(Data value)
			{
				return null;
			}
			
			@Override
			public Data getValue()
			{
				return data;
			}
			
			@Override
			public Number160 getKey()
			{
				return key;
			}
		};
		return this;
	}

	public Map<Number160, Data> getDataMap()
	{
		return dataMap;
	}

	public PutBuilder setDataMap(Map<Number160, Data> dataMap)
	{
		this.dataMap = dataMap;
		return this;
	}

	public boolean isPutIfAbsent()
	{
		return putIfAbsent;
	}

	public PutBuilder setPutIfAbsent(boolean putIfAbsent)
	{
		this.putIfAbsent = putIfAbsent;
		return this;
	}
	
	public PutBuilder putIfAbsent()
	{
		this.putIfAbsent = true;
		return this;
	}
	
	@Override
	public FutureDHT build()
	{
		preBuild("put-builder");
		if(dataMap == null)
		{
			setDataMap(new HashMap<Number160, Data>(1));
		}
		if(data != null)
		{
			getDataMap().put(getData().getKey(), getData().getValue());
		}
		if(dataMap.size()==0)
		{
			throw new IllegalArgumentException("You must either set data via setDataMap() or setData(). Cannot add nothing.");
		}
		final FutureDHT futureDHT = peer.getDistributedHashMap().put(locationKey, domainKey, dataMap, routingConfiguration, requestP2PConfiguration, 
				putIfAbsent, protectDomain, signMessage, manualCleanup, futureCreate, futureChannelCreator, peer.getConnectionBean().getConnectionReservation());
		if(directReplication)
		{
			if(defaultDirectReplication == null)
			{
				defaultDirectReplication = new DefaultDirectReplication();
			}
			Runnable runner = new Runnable()
			{
				@Override
				public void run()
				{
					FutureDHT futureDHTReplication = defaultDirectReplication.create();
					futureDHT.repeated(futureDHTReplication);
				}
			};
			ScheduledFuture<?> tmp = peer.getConnectionBean().getScheduler().getScheduledExecutorServiceReplication().scheduleAtFixedRate(
					runner, refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
			setupCancel(futureDHT, tmp);
		}
		return futureDHT;
	}
	
	private class DefaultDirectReplication implements FutureCreator<FutureDHT>
	{
		@Override
		public FutureDHT create()
		{
			final FutureChannelCreator futureChannelCreator = peer.reserve(routingConfiguration, requestP2PConfiguration, "submit-builder-direct-replication");
			FutureDHT futureDHT = peer.getDistributedHashMap().put(locationKey, domainKey, dataMap, routingConfiguration, requestP2PConfiguration, 
					putIfAbsent, protectDomain, signMessage, manualCleanup, futureCreate, futureChannelCreator, peer.getConnectionBean().getConnectionReservation());
			return futureDHT;
		}	
	}
}