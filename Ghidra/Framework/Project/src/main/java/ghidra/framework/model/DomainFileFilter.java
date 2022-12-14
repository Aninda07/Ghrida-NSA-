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
package ghidra.framework.model;

/**
 *  Interface  to indicate whether a domain file should be included in a list or
 * set of domain files.
 */
public interface DomainFileFilter {

    /**
     * Tests whether or not the specified domain file should be
     * included in a domain file list.
     *
     * @param  df  The domain file to be tested
     * @return  <code>true</code> if and only if <code>df</code>
     * 
     */
    public boolean accept(DomainFile df);

	/**
	 * Determine if linked folders represented by a link-file should be followed.
	 * If this method is not implemented the default will return {@code true}.
	 * @return true if linked-folders should be followed or false to ignore.
	 */
	public default boolean followLinkedFolders() {
		return true;
	}
}
