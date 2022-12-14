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
package ghidra.app.util.bin.format.pdb2.pdbreader;

import java.io.IOException;
import java.io.Writer;

/**
 * This class is the version of {@link TypeProgramInterface} for Microsoft v2.00 PDB.
 */
public class TypeProgramInterface200 extends TypeProgramInterface {

	//==============================================================================================
	// Internals
	//==============================================================================================
	protected int hashStreamNumber;

	//==============================================================================================
	// API
	//==============================================================================================
	/**
	 * Constructor
	 * @param pdb {@link AbstractPdb} that owns this {@link TypeProgramInterface}
	 * @param recordCategory the RecordCategory of these records
	 * @param streamNumber the stream number that contains the {@link TypeProgramInterface}
	 */
	public TypeProgramInterface200(AbstractPdb pdb, RecordCategory recordCategory,
			int streamNumber) {
		super(pdb, recordCategory, streamNumber);
	}

	//==============================================================================================
	// Abstract Methods
	//==============================================================================================
	@Override
	protected void deserializeHeader(PdbByteReader reader) throws PdbException {
		//System.out.println(reader.dump(0x100));
		versionNumber = reader.parseInt();
		headerLength = 16; // TODO: confirm this with real data.  Could be 14?
		typeIndexMin = reader.parseUnsignedShortVal();
		typeIndexMaxExclusive = reader.parseUnsignedShortVal();
		dataLength = reader.parseInt();
		hashStreamNumber = reader.parseUnsignedShortVal();
		reader.align4();
		hash.initHeader200500(hashStreamNumber, typeIndexMin, typeIndexMaxExclusive);
	}

	@Override
	protected void dumpHeader(Writer writer) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("\nversionNumber: ");
		builder.append(versionNumber);
		builder.append("\ntypeIndexMin: ");
		builder.append(typeIndexMin);
		builder.append("\ntypeIndexMaxExclusive: ");
		builder.append(typeIndexMaxExclusive);
		builder.append("\ndataLength: ");
		builder.append(dataLength);
		builder.append("\nhashStreamNumber: ");
		builder.append(hashStreamNumber);
		writer.write(builder.toString());
	}

	//==============================================================================================
	// Package-Protected Internals
	//==============================================================================================
	/**
	 * IMPORTANT: This method is for testing only.  It allows us to set a basic object.
	 * <p>
	 * Note: not all values are initialized.  This is a dummy constructor used to create a dummy
	 * {@link TypeProgramInterface}.
	 * <p>
	 * Note: not all values are initialized.
	 * @param pdb {@link AbstractPdb} that owns this {@link TypeProgramInterface}
	 * @param typeIndexMin the IndexMin to set/use
	 * @param typeIndexMaxExclusive one greater than the MaxIndex to set/use
	 */
	TypeProgramInterface200(AbstractPdb pdb, int typeIndexMin, int typeIndexMaxExclusive) {
		super(pdb, typeIndexMin, typeIndexMaxExclusive);
	}

}
