package se.kth.speech.coin.tangrams.wac.logistic;

import java.util.Map;
import java.util.function.BiFunction;

import se.kth.speech.coin.tangrams.wac.data.Round;


public enum WordProbabilityScoreDatum implements BiFunction<CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>>, WordProbabilityScorer.ReferentWordScore, String> {
	// @formatter:off
	CROSS_VALIDATION_ITER {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Integer.toString(cvResult.getCrossValidationIteration());
		}

	},
	DYAD {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
			return evalResult.getSessionId();
		}

	},
	ROUND {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
			return Integer.toString(evalResult.getRoundId());
		}

	},
	ROUND_START_TIME {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
			final Round round = evalResult.getRound();
			return Float.toString(round.getTime());
		}

	},
	GAME_SCORE {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
			final Round round = evalResult.getRound();
			return Integer.toString(round.getScore());
		}

	},
	UTT_START_TIME {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Float.toString(refWordScore.getUttStartTime());
		}
	},
	UTT_END_TIME {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Float.toString(refWordScore.getUttEndTime());
		}
	},
	TOKEN_SEQ_ORDINALITY {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Integer.toString(refWordScore.getTokSeqOrdinality());
		}
	},
	IS_INSTRUCTOR {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Boolean.toString(refWordScore.isInstructor());
		}
	},
	WORD {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return refWordScore.getWord();
		}
	},
	WORD_OBS_COUNT {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Long.toString(refWordScore.getWordObsCount());
		}
	},
	PROBABILITY {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Double.toString(refWordScore.getScore());
		}
	},
	ENTITY {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Integer.toString(refWordScore.getRefId());
		}

	},
	IS_TARGET {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Boolean.toString(refWordScore.getRef().isTarget());
		}
	},
	SHAPE {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return refWordScore.getRef().getShape();
		}

	},
	EDGE_COUNT {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Integer.toString(refWordScore.getRef().getEdgeCount());
		}

	},
	SIZE {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Double.toString(refWordScore.getRef().getSize());
		}

	},
	RED {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Float.toString(refWordScore.getRef().getRedLinear());
		}

	},
	GREEN {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Float.toString(refWordScore.getRef().getGreenLinear());
		}

	},
	BLUE {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Float.toString(refWordScore.getRef().getBlueLinear());
		}

	},
	HUE {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Float.toString(refWordScore.getRef().getHue());
		}

	},
	POSITION_X {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Double.toString(refWordScore.getRef().getPositionX());
		}

	},
	POSITION_Y {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Double.toString(refWordScore.getRef().getPositionY());
		}

	},
	MID_X {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Double.toString(refWordScore.getRef().getMidX());
		}

	},
	MID_Y {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			return Double.toString(refWordScore.getRef().getMidY());
		}

	},
	ONLY_INSTRUCTOR {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
			return modelParams.get(ModelParameter.ONLY_INSTRUCTOR).toString();
		}

	},
	RANDOM_SEED {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
			return modelParams.get(ModelParameter.RANDOM_SEED).toString();
		}

	},
	TRAINING_SET_SIZE_DISCOUNT {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
			return modelParams.get(ModelParameter.TRAINING_SET_SIZE_DISCOUNT).toString();
		}

	},
	UPDATE_WEIGHT {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
			return modelParams.get(ModelParameter.UPDATE_WEIGHT).toString();
		}

	},
	WEIGHT_BY_FREQ {

		@Override
		public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore) {
			final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
			return modelParams.get(ModelParameter.WEIGHT_BY_FREQ).toString();
		}

	};
	// @formatter:on
}
