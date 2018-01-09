/*
 * 	Copyright 2018 Todd Shore
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package se.kth.speech.coin.tangrams.keywords;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.Assert;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Jan 9, 2018
 *
 */
@RunWith(Theories.class)
public final class ReferenceDataTest {

	@DataPoints
	public static final int[] VALUES;

	static {
		VALUES = new Random().ints().map(Math::abs).limit(10).toArray();
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.coin.tangrams.keywords.ReferenceData#probabilityOfBeingCoreferentFromStart()}.
	 *
	 * @param roundCount
	 *            The total number of rounds in the game.
	 * @param distinctEntityCount
	 *            The number of distinct entities (possible referents) in the
	 *            game.
	 */
	@Theory
	public void testProbabilityOfBeingCoreferentFromStart(final int roundCount, final int distinctEntityCount) {
		final ReferenceData data = new ReferenceData(2000, 5);
		final BigDecimal prob = data.probabilityOfBeingCoreferentFromStart();
		Assert.assertTrue("Probability is less than 0.", prob.compareTo(BigDecimal.ZERO) >= 0);
		Assert.assertTrue("Probability is greater than 1.", prob.compareTo(BigDecimal.ONE) <= 0);
	}

}
