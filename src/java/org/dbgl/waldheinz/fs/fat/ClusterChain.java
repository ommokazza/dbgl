/*
 * Copyright (C) 2009,2010 Matthias Treydte <mt@waldheinz.de>
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.dbgl.waldheinz.fs.fat;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * A chain of clusters as stored in a {@link Fat}.
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ClusterChain {

	protected final Fat fat;
	private final BlockDevice device;
	private final int clusterSize;
	protected final long dataOffset;
	private long startCluster;

	public ClusterChain(Fat fat, long startCluster) {
		this.fat = fat;

		if (startCluster != 0) {
			this.fat.testCluster(startCluster);

			if (this.fat.isFreeCluster(startCluster))
				throw new IllegalArgumentException("cluster " + startCluster + " is free");
		}

		this.device = fat.getDevice();
		this.dataOffset = fat.getBootSector().getFilesOffset();
		this.startCluster = startCluster;
		this.clusterSize = fat.getBootSector().getBytesPerCluster();
	}

	public int getClusterSize() {
		return clusterSize;
	}

	public Fat getFat() {
		return fat;
	}

	public BlockDevice getDevice() {
		return device;
	}

	/**
	 * Returns the first cluster of this chain.
	 * 
	 * @return the chain's first cluster, which may be 0 if this chain does not contain any clusters
	 */
	public long getStartCluster() {
		return startCluster;
	}

	/**
	 * Calculates the device offset (0-based) for the given cluster and offset within the cluster.
	 * 
	 * @param cluster
	 * @param clusterOffset
	 * @return long
	 * @throws FileSystemException
	 */
	private long getDevOffset(long cluster, int clusterOffset) {
		return dataOffset + clusterOffset + ((cluster - Fat.FIRST_CLUSTER) * clusterSize);
	}

	/**
	 * Returns the size this {@code ClusterChain} occupies on the device.
	 * 
	 * @return the size this chain occupies on the device in bytes
	 */
	public long getLengthOnDisk() {
		if (getStartCluster() == 0)
			return 0;

		return getChainLength() * clusterSize;
	}

	/**
	 * Determines the length of this {@code ClusterChain} in clusters.
	 * 
	 * @return the length of this chain
	 */
	public int getChainLength() {
		if (getStartCluster() == 0)
			return 0;

		final long[] chain = getFat().getChain(getStartCluster());
		return chain.length;
	}

	public void readData(long offset, ByteBuffer dest) throws IOException {

		int len = dest.remaining();

		if ((startCluster == 0 && len > 0))
			throw new EOFException();

		final long[] chain = getFat().getChain(startCluster);
		final BlockDevice dev = getDevice();

		int chainIdx = (int)(offset / clusterSize);
		if (offset % clusterSize != 0) {
			int clusOfs = (int)(offset % clusterSize);
			int size = Math.min(len, (int)(clusterSize - (offset % clusterSize) - 1));
			dest.limit(dest.position() + size);

			dev.read(getDevOffset(chain[chainIdx], clusOfs), dest);

			len -= size;
			chainIdx++;
		}

		while (len > 0) {
			int size = Math.min(clusterSize, len);
			dest.limit(dest.position() + size);

			dev.read(getDevOffset(chain[chainIdx], 0), dest);

			len -= size;
			chainIdx++;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof ClusterChain))
			return false;

		final ClusterChain other = (ClusterChain)obj;

		if (this.fat != other.fat && (this.fat == null || !this.fat.equals(other.fat))) {

			return false;
		}

		if (this.startCluster != other.startCluster) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 79 * hash + (this.fat != null ? this.fat.hashCode(): 0);
		hash = 79 * hash + (int)(this.startCluster ^ (this.startCluster >>> 32));
		return hash;
	}

}
