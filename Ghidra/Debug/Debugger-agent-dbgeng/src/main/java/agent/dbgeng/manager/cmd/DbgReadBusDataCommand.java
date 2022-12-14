/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package agent.dbgeng.manager.cmd;

import java.nio.ByteBuffer;

import agent.dbgeng.manager.DbgThread;
import agent.dbgeng.manager.impl.DbgManagerImpl;

/**
 * Implementation of {@link DbgThread#readMemory(long, ByteBuffer, int)}
 */
public class DbgReadBusDataCommand extends AbstractDbgReadCommand {

	private final int busDataType;
	private final int busNumber;
	private final int slotNumber;

	public DbgReadBusDataCommand(DbgManagerImpl manager, long addr, ByteBuffer buf, int len,
			int busDataType, int busNumber, int slotNumber) {
		super(manager, addr, buf, len);
		this.busDataType = busDataType;
		this.busNumber = busNumber;
		this.slotNumber = slotNumber;
	}

	@Override
	protected int doRead(long addr, ByteBuffer buf, int len) {
		return manager.getDataSpaces()
				.readBusData(busDataType, busNumber, slotNumber, addr, buf, len);
	}
}
