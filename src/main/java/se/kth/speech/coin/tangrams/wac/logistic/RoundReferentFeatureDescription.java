/*
 * 	Copyright 2017 Todd Shore
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
package se.kth.speech.coin.tangrams.wac.logistic;

import java.util.function.Function;

import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 6 Dec 2017
 *
 */
enum RoundReferentFeatureDescription implements Function<RoundReferentFeatureDescription.Input, String> {
	// @formatter:off
	DYAD {

		@Override
		public String apply(final Input input) {
			return input.dyadId;
		}

	},
	ROUND {

		@Override
		public String apply(final Input input) {
			return input.roundId;
		}

	},
	ENTITY {

		@Override
		public String apply(final Input input) {
			return input.refId;
		}

	},
	TARGET {

		@Override
		public String apply(final Input input) {
			return Boolean.toString(input.ref.isTarget());
		}

	},
	CONFIDENCE {


		@Override
		public String apply(final Input input) {
			return Double.toString(input.confidence);
		}

	},
	SCORE {

		@Override
		public String apply(final Input input) {
			return Integer.toString(input.round.getScore());
		}

	},
	ROUND_START_TIME {

		@Override
		public String apply(final Input input) {
			return Float.toString(input.round.getTime());
		}

	},
	WORD {

		@Override
		public String apply(final Input input) {
			return input.word;
		}

	},
	IS_OOV {

		@Override
		public String apply(final Input input) {
			return Boolean.toString(input.isOov);
		}

	},
	SHAPE {

		@Override
		public String apply(final Input input) {
			return input.ref.getShape();
		}

	},
	EDGE_COUNT {

		@Override
		public String apply(final Input input) {
			return Integer.toString(input.ref.getEdgeCount());
		}

	},
	SIZE {

		@Override
		public String apply(final Input input) {
			return Double.toString(input.ref.getSize());
		}

	},
	RED {

		@Override
		public String apply(final Input input) {
			return Float.toString(input.ref.getRedLinear());
		}

	},
	GREEN {

		@Override
		public String apply(final Input input) {
			return Float.toString(input.ref.getGreenLinear());
		}

	},
	BLUE {

		@Override
		public String apply(final Input input) {
			return Float.toString(input.ref.getBlueLinear());
		}

	},
	HUE {

		@Override
		public String apply(final Input input) {
			return Float.toString(input.ref.getHue());
		}

	},
	POSITION_X {

		@Override
		public String apply(final Input input) {
			return Double.toString(input.ref.getPositionX());
		}

	},
	POSITION_Y {

		@Override
		public String apply(final Input input) {
			return Double.toString(input.ref.getPositionY());
		}

	},
	MID_X {

		@Override
		public String apply(final Input input) {
			return Double.toString(input.ref.getMidX());
		}

	},
	MID_Y {

		@Override
		public String apply(final Input input) {
			return Double.toString(input.ref.getMidY());
		}

	};	
	// @formatter:on

	static final class Input {

		private final String dyadId;

		private final String roundId;

		private final Round round;
		
		private final String refId;

		private final Referent ref;

		private final String word;
		
		private final boolean isOov;

		private final double confidence;

		Input(final String dyadId, final String roundId, final Round round, String refId, final Referent ref, final String word, boolean isOov,
				final double confidence) {
			this.dyadId = dyadId;
			this.roundId = roundId;
			this.round = round;
			this.refId = refId;
			this.ref = ref;
			this.word = word;
			this.isOov = isOov;
			this.confidence = confidence;
		}

	}

}
