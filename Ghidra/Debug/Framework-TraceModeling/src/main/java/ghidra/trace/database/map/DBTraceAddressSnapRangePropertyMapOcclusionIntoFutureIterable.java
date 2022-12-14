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
package ghidra.trace.database.map;

import ghidra.trace.model.Lifespan;
import ghidra.trace.model.TraceAddressSnapRange;
import ghidra.util.database.spatial.rect.Rectangle2DDirection;

public class DBTraceAddressSnapRangePropertyMapOcclusionIntoFutureIterable<T>
		extends AbstractDBTraceAddressSnapRangePropertyMapOcclusionIterable<T> {

	public DBTraceAddressSnapRangePropertyMapOcclusionIntoFutureIterable(
			DBTraceAddressSnapRangePropertyMapSpace<T, ?> space, TraceAddressSnapRange within) {
		super(space, within);
	}

	@Override
	protected Rectangle2DDirection getVerticalDirection() {
		return Rectangle2DDirection.BOTTOMMOST;
	}

	@Override
	protected Lifespan getOcclusionRange(Lifespan range) {
		long lowerEndpoint = range.lmin();
		if (lowerEndpoint == Long.MIN_VALUE) {
			return null;
		}
		return Lifespan.span(Long.MIN_VALUE, lowerEndpoint - 1);
	}
}
