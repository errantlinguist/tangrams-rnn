package se.kth.speech.coin.tangrams.data;

import java.util.Objects;

public final class SpeakerToken {

	private final boolean isInstructor;

	private final String token;

	SpeakerToken(final String token, final boolean isInstructor) {
		this.token = token;
		this.isInstructor = isInstructor;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SpeakerToken that = (SpeakerToken) o;
		return isInstructor == that.isInstructor &&
				Objects.equals(token, that.token);
	}

	@Override
	public int hashCode() {
		return Objects.hash(isInstructor, token);
	}

	@Override
	public String toString() {
		return "SpeakerToken{" +
				"isInstructor=" + isInstructor +
				", token='" + token + '\'' +
				'}';
	}

	public boolean isInstructor() {
		return isInstructor;
	}

	public String getToken() {
		return token;
	}
}
