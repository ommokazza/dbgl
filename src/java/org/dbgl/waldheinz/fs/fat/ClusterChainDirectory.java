/*
 * Copyright (C) 2003-2009 JNode.org
 *               2009,2010 Matthias Treydte <mt@waldheinz.de>
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

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * A directory that is stored in a cluster chain.
 * 
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
class ClusterChainDirectory extends AbstractDirectory {

	/**
	 * According to the FAT specification, this is the maximum size a FAT directory may occupy on disk. The {@code ClusterChainDirectory} takes care not to grow beyond this limit.
	 * 
	 * @see #changeSize(int)
	 */
	public final static int MAX_SIZE = 65536 * 32;

	/**
	 * The {@code ClusterChain} that stores this directory. Package-visible for testing.
	 */
	final ClusterChain chain;

	protected ClusterChainDirectory(ClusterChain chain, boolean isRoot) {
		super((int)(chain.getLengthOnDisk() / FatDirectoryEntry.SIZE), isRoot);
		this.chain = chain;
	}

	public static ClusterChainDirectory readRoot(ClusterChain chain) throws IOException {
		final ClusterChainDirectory result = new ClusterChainDirectory(chain, true);
		result.read();
		return result;
	}

	@Override
	protected final void read(ByteBuffer data) throws IOException {
		this.chain.readData(0, data);
	}
}
