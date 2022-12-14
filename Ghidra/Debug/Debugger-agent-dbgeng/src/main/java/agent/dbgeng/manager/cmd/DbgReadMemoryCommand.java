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
public class DbgReadMemoryCommand extends AbstractDbgReadCommand {

	public DbgReadMemoryCommand(DbgManagerImpl manager, long addr, ByteBuffer buf, int len) {
		super(manager, addr, buf, len);
	}

	@Override
	protected int doRead(long addr, ByteBuffer buf, int len) {
		return manager.getDataSpaces().readVirtual(addr, buf, len);
	}
}
