package com.ycg.rdc.framework.support.id.algorithm.snowflake;

import java.util.concurrent.ThreadLocalRandom;

public class Sequence implements ISequence {

	private final long twepoch = 1288834974657L;

	private final long workerIdBits = 5L;

	private final long datacenterIdBits = 5L;

	private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

	private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

	private final long sequenceBits = 12L;

	private final long workerIdShift = sequenceBits;

	private final long datacenterIdShift = sequenceBits + workerIdBits;

	private final long timestampLeftShift = sequenceBits + workerIdBits
			+ datacenterIdBits;

	private final long sequenceMask = -1L ^ (-1L << sequenceBits);

	private long workerId;

	private long datacenterId;

	private long sequence = 0L;

	private long lastTimestamp = -1L;
	
	private long id = 0L;

	public Sequence(long workerId, long datacenterId) {
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format(
					"worker Id can't be greater than %d or less than 0",
					maxWorkerId));
		}

		if (datacenterId > maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(String.format(
					"datacenter Id can't be greater than %d or less than 0",
					maxDatacenterId));
		}

		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}

	public synchronized long nextId() {
		long timestamp = timeGen();
		if (timestamp < lastTimestamp) {
			long offset = lastTimestamp - timestamp;
			if (offset <= 5) {
				try {
					wait(offset << 1);
					timestamp = timeGen();
					if (timestamp < lastTimestamp) {
						throw new RuntimeException(
								String.format(
										"Clock moved backwards.  Refusing to generate id for %d milliseconds",
										offset));
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				throw new RuntimeException(
						String.format(
								"Clock moved backwards.  Refusing to generate id for %d milliseconds",
								offset));
			}
		}

		if (lastTimestamp == timestamp) {
			long old = sequence;
			sequence = (sequence + 1) & sequenceMask;
			if (sequence == old) {
				timestamp = tilNextMillis(lastTimestamp);
			}
		} else {
			sequence = ThreadLocalRandom.current().nextLong(0, 2);
		}

		lastTimestamp = timestamp;

		id = ((timestamp - twepoch) << timestampLeftShift)
				| (datacenterId << datacenterIdShift)
				| (workerId << workerIdShift) | sequence;
		return id;
	}

	public synchronized long nowId()
	{
		return id;
	}
	protected long tilNextMillis(long lastTimestamp) {
		long timestamp = timeGen();
		while (timestamp <= lastTimestamp) {
			timestamp = timeGen();
		}

		return timestamp;
	}

	protected long timeGen() {
		return SystemClock.now();
	}

}