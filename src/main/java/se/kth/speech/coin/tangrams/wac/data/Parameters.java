/*******************************************************************************
 * Copyright 2017 Todd Shore
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package se.kth.speech.coin.tangrams.wac.data;

import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parameters {
	
	// Only build model for words with more or equal number of instances than this
	public static int DISCOUNT = 3;
	
	// Only use referring language
	public static boolean ONLY_REFLANG = false;
	
	// Only use language from the giver 
	public static boolean ONLY_GIVER = false;
	
	// Weight score by word frequency 
	public static boolean WEIGHT_BY_FREQ = true;
		
	// Update the model incrementally during testing	
	public static boolean UPDATE_MODEL = false;
	
	// Weight for incremental updates (relative to 1.0 for background model)
	public static double UPDATE_WEIGHT = 1.0;

	public static String getSetting() {
		return Stream.of(DISCOUNT, ONLY_REFLANG, ONLY_GIVER, UPDATE_MODEL, UPDATE_WEIGHT).map(Object::toString).collect(ROW_CELL_JOINER);
	}
	
	private static final String HEADER;
	
	private static final Collector<CharSequence,?,String> ROW_CELL_JOINER = Collectors.joining("\t");
	
	static {
		HEADER = Stream.of("DISCOUNT", "ONLY_REFLANG", "ONLY_GIVER", "UPDATE_MODEL", "UPDATE_WEIGHT").collect(ROW_CELL_JOINER);
	}

	/**
	 * @return the header
	 */
	public static String getHeader() {
		return HEADER;
	}
	
}
