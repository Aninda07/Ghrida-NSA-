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
package agent.frida.manager.cmd;

import java.nio.ByteBuffer;

import agent.frida.manager.impl.FridaManagerImpl;
import ghidra.program.model.address.Address;

public class FridaReadKernelMemoryCommand extends AbstractFridaReadCommand {

	public FridaReadKernelMemoryCommand(FridaManagerImpl manager, Address addr, ByteBuffer buf,
			int len) {
		super(manager, addr, buf, len);
	}

	@Override
	public void invoke() {
		manager.loadScript(this, "read_memory",
			"var buf = Kernel.readByteArray(ptr(0x" + addr + ")," + len +
				"); result = hexdump(buf, {header:false});");
	}

}
